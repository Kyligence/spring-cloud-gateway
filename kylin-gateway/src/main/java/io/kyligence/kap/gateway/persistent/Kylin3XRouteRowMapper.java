package io.kyligence.kap.gateway.persistent;

import io.kyligence.kap.gateway.persistent.domain.Kylin3XRouteDO;
import lombok.val;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Kylin3XRouteRowMapper implements RowMapper<Kylin3XRouteDO> {

	@Override
	public Kylin3XRouteDO mapRow(ResultSet rs, int rowNum) throws SQLException {
		val id = rs.getLong(1);
		val backends = rs.getString(2);
		val project = rs.getString(3);
		val resourceGroup = rs.getString(4);
		val type = rs.getString(5);
		val cluster = rs.getString(6);
		return new Kylin3XRouteDO(id, backends, project, resourceGroup, type, cluster);
	}

}
