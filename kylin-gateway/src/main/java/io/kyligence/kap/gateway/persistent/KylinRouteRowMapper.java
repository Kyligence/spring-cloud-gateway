package io.kyligence.kap.gateway.persistent;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.kyligence.kap.gateway.persistent.domain.KylinRouteTableDO;
import io.kyligence.kap.gateway.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Slf4j
public class KylinRouteRowMapper implements RowMapper<KylinRouteTableDO> {

	@Override
	public KylinRouteTableDO mapRow(ResultSet rs, int rowNum) throws SQLException {
		val id = rs.getLong(1);
		val clusterId = rs.getString(2);
		val serviceId = rs.getString(3);
		val routeTableStr = rs.getString(4);

		List<KylinRouteTableDO.KylinRouteDO> routeTable = Lists.newArrayList();

		try {
			Preconditions.checkNotNull(routeTableStr, "Jdbc route table is null !");

			KylinRouteTableDO.KylinRouteDO[] routeArray = JsonUtil.toObject(routeTableStr, KylinRouteTableDO.KylinRouteDO[].class);

			Preconditions.checkNotNull(routeArray, "Parse route table failed, return null !");

			for (int i = 0; i < routeArray.length; i++) {
				routeArray[i].setId(1L + i);
				routeTable.add(routeArray[i]);
			}
		} catch (Exception e) {
			log.error("Failed to parse route table !", e);
		}

		return new KylinRouteTableDO(id, clusterId, serviceId, routeTable);
	}

}
