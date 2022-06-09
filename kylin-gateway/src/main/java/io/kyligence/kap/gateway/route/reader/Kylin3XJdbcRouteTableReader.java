package io.kyligence.kap.gateway.route.reader;

import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import io.kyligence.kap.gateway.persistent.Kylin3XRouteRowMapper;
import io.kyligence.kap.gateway.persistent.domain.Kylin3XRouteDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Kylin3XJdbcRouteTableReader implements IRouteTableReader {

	private static final String ID = "id";

	private static final String BACKENDS = "backends";

	private static final String PROJECT = "project";

	private static final String RESOURCE_GROUP = "resource_group";

	private static final String TYPE = "type";

	private static final String CLUSTER = "cluster";

	public static final String SELECT_ALL_ROUTES = "select "
			+ String.join(",", ID, BACKENDS, PROJECT, RESOURCE_GROUP, TYPE, CLUSTER)
			+ " from %s";

	private final JdbcTemplate jdbcTemplate;

	private final String table;

	public Kylin3XJdbcRouteTableReader(JdbcTemplate jdbcTemplate, String table) {
		this.jdbcTemplate = jdbcTemplate;
		this.table = StringUtils.replace(table, "'", "");
	}

	@Override
	public List<KylinRouteRaw> list() {
		log.debug("Start to read route table from jdbc ...");
		List<Kylin3XRouteDO> kylinRouteDOList = jdbcTemplate.query(String.format(SELECT_ALL_ROUTES, table),
				new Kylin3XRouteRowMapper());
		return kylinRouteDOList.stream().map(KylinRouteRaw::convert).collect(Collectors.toList());
	}

}
