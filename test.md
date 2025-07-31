%% 开启流程图/架构图模式
flowchart LR
    %% 定义节点（样式可自定义）
    subgraph ShellExtension(C++)
        A[ContextMenu<br/>右键菜单集成]
        B[Overlays<br/>图标覆盖层]
        C[Common<br/>HTTP通信模块]
    end

    subgraph LocalProxy(Go)
        D[LocalProxy<br/>HTTP REST]
        E[Cache Manager<br/>5分钟TTL]
        F[Performance Stats<br/>命中率统计]
        G[HTTP Server :8080]
    end

    subgraph RemoteServer(Java)
        H[Spring Boot API<br/>RESTful Services]
        I[H2 Database<br/>文件状态存储]
        J[Health Monitoring<br/>Actuator端点]
    end

    %% 定义监控端点（用注释/单独节点）
    %% 监控端点
    M1[LocalProxy Health<br/>http://localhost:8080/api/v1/health]
    M2[RemoteServer Health<br/>http://localhost:8080/actuator/health]
    M3[Cache Statistics<br/>http://localhost:8080/api/v1/stats]
    
    %% 性能指标（用注释/单独节点）
    P1[响应时间: 1-5ms<br/>(缓存命中时)]
    P2[缓存命中率: 90%+]
    P3[离线支持: ✅]

    %% 连线关系
    %% ShellExtension -> LocalProxy
    C --> D
    A --> D
    B --> D

    %% LocalProxy 内部
    D --> G
    E --> G
    F --> G

    %% LocalProxy -> RemoteServer（缓存未命中分支）
    G -->|缓存未命中| H

    %% RemoteServer 内部
    H --> I
    J --> H

    %% 监控/性能指标 可放右侧或单独排版
    %% 这里用单独节点示意，实际可放侧边
