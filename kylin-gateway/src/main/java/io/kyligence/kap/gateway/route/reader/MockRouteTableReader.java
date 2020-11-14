package io.kyligence.kap.gateway.route.reader;

import com.google.common.collect.Lists;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;
import io.kyligence.kap.gateway.persistent.domain.Kylin3XRouteDO;

import java.util.List;

public class MockRouteTableReader implements IRouteTableReader {

	@Override
	public List<KylinRouteRaw> list() {
		List<KylinRouteRaw> kylinRouteRawList = Lists.newArrayList();

		kylinRouteRawList.add(KylinRouteRaw.convert(new Kylin3XRouteDO(1, "[\"10.1.2.56:7070\"]",
				"p1", "common_query_1", "QUERY", "1c4b3f35-21f9-44b2-a2de-ae2d5a94189f")));

		kylinRouteRawList.add(KylinRouteRaw.convert(new Kylin3XRouteDO(2, "[\"10.1.2.56:7070\"]",
				"p1", "common_query_2", "BUILD", "1c4b3f35-21f9-44b2-a2de-ae2d5a94189f")));

		kylinRouteRawList.add(KylinRouteRaw.convert(new Kylin3XRouteDO(3, "[\"10.1.2.56:7070\", \"10.1.1.1:7070\"]",
				"p2", "default", "GLOBAL", "1c4b3f35-21f9-44b2-a2de-ae2d5a94189f")));

		kylinRouteRawList.add(KylinRouteRaw.convert(new Kylin3XRouteDO(4, "[\"10.1.2.56:7070\"]",
				"p2", "common_query_1", "QUERY", "1c4b3f35-21f9-44b2-a2de-ae2d5a94189f")));

		return kylinRouteRawList;
	}

}
