package io.kyligence.kap.gateway.config;

import lombok.Data;

@Data
public class KylinJdbcDataSource {

	private String driverClassName;

	private String url;

	private String username;

	private String password;

	private String tableName;

}
