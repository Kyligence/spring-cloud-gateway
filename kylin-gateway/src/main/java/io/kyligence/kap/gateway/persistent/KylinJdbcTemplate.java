package io.kyligence.kap.gateway.persistent;

import lombok.Data;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Data
public class KylinJdbcTemplate extends JdbcTemplate {

	private String tableName;

	public KylinJdbcTemplate(DataSource dataSource, String tableName) {
		super(dataSource);
		this.tableName = tableName;
	}

}
