package org.example.matrix.index;

import cn.hutool.extra.spring.SpringUtil;
import lombok.experimental.UtilityClass;
import org.example.matrix.services.ServiceProxyFactory;

/**
 * 索引工具
 *
 * @author pinkman
 * @since 1.0
 */
@UtilityClass
public class IndexHelper {

    public static final IndexService service = new ServiceProxyFactory().createProxy(IndexService.class, () -> SpringUtil.getBean(IndexService.class));

}
