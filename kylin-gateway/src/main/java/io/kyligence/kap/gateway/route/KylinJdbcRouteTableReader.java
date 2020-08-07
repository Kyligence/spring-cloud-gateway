package io.kyligence.kap.gateway.route;

import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import io.kyligence.kap.gateway.persistent.KylinRouteRowMapper;
import io.kyligence.kap.gateway.persistent.domain.KylinRouteDO;
import io.kyligence.kap.gateway.route.IRouteTableReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class KylinJdbcRouteTableReader implements IRouteTableReader {

	private static final String ID = "id";

	private static final String BACKENDS = "backends";

	private static final String PROJECT = "project";

	private static final String RESOURCE_GROUP = "resource_group";

	private static final String TYPE = "type";

	private static final String CLUSTER = "cluster";

	public static final String SELECT_ALL_ROUTES = "select "
			+ String.join(",", ID, BACKENDS, PROJECT, RESOURCE_GROUP, TYPE, CLUSTER)
			+ " from %s";

	private JdbcTemplate jdbcTemplate;

	private String table;

	public KylinJdbcRouteTableReader(JdbcTemplate jdbcTemplate, String table) {
		this.jdbcTemplate = jdbcTemplate;
		this.table = table;
	}

	@Override
	public List<KylinRouteRaw> list() {
		log.debug("list route records");
		List<KylinRouteDO> kylinRouteDOList = jdbcTemplate.query(String.format(SELECT_ALL_ROUTES, table),
				new KylinRouteRowMapper());
		return kylinRouteDOList.stream().map(KylinRouteRaw::new).collect(Collectors.toList());
	}

}
