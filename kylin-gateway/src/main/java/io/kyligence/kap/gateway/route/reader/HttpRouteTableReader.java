package io.kyligence.kap.gateway.route.reader;

import com.google.common.collect.Lists;
import io.kyligence.kap.gateway.entity.KylinRouteRaw;

import java.util.List;

// TODO
public class HttpRouteTableReader implements IRouteTableReader {

	@Override
	public List<KylinRouteRaw> list() {
		return Lists.newArrayList();
	}

}
