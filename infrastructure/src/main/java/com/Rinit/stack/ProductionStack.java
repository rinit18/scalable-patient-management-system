package com.Rinit.stack;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.elasticache.CfnCacheCluster;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductionStack extends Stack {

    private final Vpc vpc;
    private final Cluster ecsCluster;
    private final SecurityGroup internalCommunicationSg;

    public ProductionStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = createVpc();

        // Security Group for internal microservice communication
        this.internalCommunicationSg = SecurityGroup.Builder.create(this, "InternalSg")
                .vpc(vpc)
                .description("Allow internal cluster communication")
                .allowAllOutbound(true)
                .build();
        internalCommunicationSg.addIngressRule(Peer.ipv4(vpc.getVpcCidrBlock()), Port.allTraffic());

        // 1. Databases (Set to RETAIN for Production)
        DatabaseInstance authServiceDb = createDatabase("AuthServiceDB", "auth_db");
        DatabaseInstance patientServiceDb = createDatabase("PatientServiceDB", "patient_db");

        // 2. Event Streaming (Downsized to t3.small to prevent massive AWS bills)
        CfnCluster mskCluster = createMskCluster();

        // 3. Caching (Added ElastiCache Redis for Patient Service)
        CfnCacheCluster redisCluster = createRedisCache();

        // 4. ECS Cluster with Service Discovery
        this.ecsCluster = createEcsCluster();

        // 5. Microservices (Pulling from ECR instead of local Docker)
        FargateService authService = createFargateService("AuthService", "auth-service", List.of(4005), authServiceDb,
                Map.of("JWT_SECRET", "YOUR_PRODUCTION_SECRET_KEY_HERE"));

        FargateService billingService = createFargateService("BillingService", "billing-service", List.of(4001, 9001), null, null);

        FargateService analyticsService = createFargateService("AnalyticsService", "analytics-service", List.of(4002), null, null);

        // 6. Patient Service (Fixed gRPC Host and added Redis configurations)
        FargateService patientService = createFargateService("PatientService", "patient-service", List.of(4000), patientServiceDb,
                Map.of(
                        "BILLING_SERVICE_ADDRESS", "billing-service.patient-management.local", // Fixed internal DNS
                        "BILLING_SERVICE_GRPC_PORT", "9001",
                        "SPRING_CACHE_TYPE", "redis",
                        "SPRING_DATA_REDIS_HOST", redisCluster.getAttrRedisEndpointAddress(),
                        "SPRING_DATA_REDIS_PORT", redisCluster.getAttrRedisEndpointPort()
                ));

        createApiGatewayService();
    }

    private Vpc createVpc() {
        return Vpc.Builder.create(this, "PatientManagementProdVPC")
                .maxAzs(2)
                .natGateways(1) // Required for private subnet internet access (pulling images)
                .build();
    }

    private DatabaseInstance createDatabase(String id, String dbName) {
        return DatabaseInstance.Builder.create(this, id)
                .engine(DatabaseInstanceEngine.postgres(
                        PostgresInstanceEngineProps.builder().version(PostgresEngineVersion.VER_15_4).build()))
                .vpc(vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
                .allocatedStorage(20)
                .credentials(Credentials.fromGeneratedSecret("postgres"))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.RETAIN) // CRITICAL FOR PROD
                .securityGroups(List.of(internalCommunicationSg))
                .build();
    }

    private CfnCacheCluster createRedisCache() {
        return CfnCacheCluster.Builder.create(this, "PatientRedisCache")
                .cacheNodeType("cache.t3.micro")
                .engine("redis")
                .numCacheNodes(1)
                .vpcSecurityGroupIds(List.of(internalCommunicationSg.getSecurityGroupId()))
                .build();
    }

    private CfnCluster createMskCluster() {
        return CfnCluster.Builder.create(this, "MskProdCluster")
                .clusterName("kafka-prod-cluster")
                .kafkaVersion("3.5.1")
                .numberOfBrokerNodes(2)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType("kafka.t3.small") // Cost-effective for portfolios
                        .clientSubnets(vpc.getPrivateSubnets().stream()
                                .map(ISubnet::getSubnetId)
                                .collect(Collectors.toList()))
                        .securityGroups(List.of(internalCommunicationSg.getSecurityGroupId()))
                        .build())
                .build();
    }

    private Cluster createEcsCluster() {
        return Cluster.Builder.create(this, "PatientManagementProdCluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("patient-management.local")
                        .build())
                .build();
    }

    private FargateService createFargateService(String id, String serviceName, List<Integer> ports, DatabaseInstance db, Map<String, String> additionalEnvVars) {
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, id + "Task")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        // Use ECR instead of local registry
        IRepository ecrRepository = Repository.fromRepositoryName(this, id + "Repo", serviceName);

        ContainerDefinitionOptions.Builder containerOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromEcrRepository(ecrRepository, "latest"))
                .portMappings(ports.stream().map(port -> PortMapping.builder()
                        .containerPort(port).hostPort(port).protocol(Protocol.TCP).build()).toList())
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, id + "LogGroup")
                                .logGroupName("/ecs/prod/" + serviceName)
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_WEEK).build())
                        .streamPrefix(serviceName).build()));

        Map<String, String> envVars = new HashMap<>();
        // In a real prod MSK setup, you fetch these brokers dynamically, but for simplicity:
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "b-1.kafkaprodcluster.xxxx.c2.kafka.us-east-1.amazonaws.com:9092");

        if (additionalEnvVars != null) envVars.putAll(additionalEnvVars);

        if (db != null) {
            envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s/%s".formatted(
                    db.getDbInstanceEndpointAddress(), db.getDbInstanceEndpointPort(), db.getInstanceIdentifier()));
            envVars.put("SPRING_DATASOURCE_USERNAME", "postgres");
            envVars.put("SPRING_DATASOURCE_PASSWORD", db.getSecret().secretValueFromJson("password").toString());
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
        }

        containerOptions.environment(envVars);
        taskDefinition.addContainer(serviceName + "Container", containerOptions.build());

        return FargateService.Builder.create(this, id)
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false) // Private subnets for security
                .securityGroups(List.of(internalCommunicationSg))
                .serviceName(serviceName)
                .cloudMapOptions(CloudMapOptions.builder().name(serviceName).build())
                .build();
    }

    private void createApiGatewayService() {
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, "APIGatewayTask")
                .cpu(256).memoryLimitMiB(512).build();

        IRepository ecrRepository = Repository.fromRepositoryName(this, "ApiGatewayRepo", "api-gateway");

        taskDefinition.addContainer("APIGatewayContainer", ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromEcrRepository(ecrRepository, "latest"))
                .environment(Map.of(
                        "SPRING_PROFILES_ACTIVE", "prod",
                        "AUTH_SERVICE_URL", "http://auth-service.patient-management.local:4005"
                ))
                .portMappings(List.of(PortMapping.builder().containerPort(4004).build()))
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, "ApiGatewayLogGroup")
                                .logGroupName("/ecs/prod/api-gateway")
                                .retention(RetentionDays.ONE_WEEK).build())
                        .streamPrefix("api-gateway").build()))
                .build());

        ApplicationLoadBalancedFargateService.Builder.create(this, "APIGatewayService")
                .cluster(ecsCluster)
                .serviceName("api-gateway")
                .taskDefinition(taskDefinition)
                .desiredCount(1)
                .publicLoadBalancer(true) // Public facing ALB
                .build();
    }
}