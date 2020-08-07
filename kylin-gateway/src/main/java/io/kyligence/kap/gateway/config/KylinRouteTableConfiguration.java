package io.kyligence.kap.gateway.config;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import io.kyligence.kap.gateway.route.IRouteTableReader;
import io.kyligence.kap.gateway.route.KylinJdbcRouteTableReader;
import io.kyligence.kap.gateway.persistent.KylinJdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties
public class KylinRouteTableConfiguration {

	@Bean
	@ConditionalOnProperty(name = "kylin.gateway.datasource.type", havingValue = "jdbc")
	@ConfigurationProperties(prefix = "kylin.gateway.datasource")
	public KylinJdbcDataSource kylinJdbcDataSource() {
		return new KylinJdbcDataSource();
	}

	@Bean
	@ConditionalOnProperty(name = "kylin.gateway.datasource.type", havingValue = "jdbc")
	public IRouteTableReader kylinRouteStore(KylinJdbcDataSource kylinJdbcDataSource) {
		DataSource dataSource = DataSourceBuilder.create().type(MysqlDataSource.class)
				.driverClassName(kylinJdbcDataSource.getDriverClassName())
				.url(kylinJdbcDataSource.getUrl())
				.username(kylinJdbcDataSource.getUsername())
				.password(kylinJdbcDataSource.getPassword()).build();
		return new KylinJdbcRouteTableReader(
				new KylinJdbcTemplate(dataSource, kylinJdbcDataSource.getTableName()),
				kylinJdbcDataSource.getTableName());
	}

}
