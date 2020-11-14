package io.kyligence.kap.gateway.entity;

import lombok.Data;

@Data
public class KylinJdbcDataSource {

	private String driverClassName;

	private String url;

	private String username;

	private String password;

	private String tableName;

	private String clusterId;

}
