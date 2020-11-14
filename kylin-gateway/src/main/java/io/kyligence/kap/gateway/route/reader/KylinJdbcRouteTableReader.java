package io.kyligence.kap.gateway.route.reader;

import com.google.common.collect.Lists;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import io.kyligence.kap.gateway.persistent.KylinRouteRowMapper;
import io.kyligence.kap.gateway.persistent.domain.KylinRouteTableDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class KylinJdbcRouteTableReader implements IRouteTableReader {

	private static final String ID = "id";

	private static final String CLUSTER_ID = "cluster_id";

	private static final String SERVICE = "service";

	private static final String ROUTE_TABLE = "info";

	public static final String SELECT_ALL_ROUTES = "select "
			+ String.join(",", ID, CLUSTER_ID, SERVICE, ROUTE_TABLE)
			+ " from %s WHERE " + SERVICE + "='GATEWAY' and " + CLUSTER_ID + " ='%s' ";

	private JdbcTemplate jdbcTemplate;

	private String table;

	private String clusterId;

	public KylinJdbcRouteTableReader(JdbcTemplate jdbcTemplate, String table, String clusterId) {
		this.jdbcTemplate = jdbcTemplate;
		this.table = table;
		this.clusterId = StringUtils.replace(clusterId, "\'", "");
	}

	@Override
	public List<KylinRouteRaw> list() {
		log.debug("Start to read route table from jdbc ...");
		List<KylinRouteTableDO> routeTableDOList = jdbcTemplate.query(String.format(SELECT_ALL_ROUTES, table, clusterId),
				new KylinRouteRowMapper());

		if (CollectionUtils.isEmpty(routeTableDOList)) {
			return Lists.newArrayList();
		}

		return routeTableDOList.get(0).getRouteTable().stream().map(KylinRouteRaw::convert).collect(Collectors.toList());
	}
}
