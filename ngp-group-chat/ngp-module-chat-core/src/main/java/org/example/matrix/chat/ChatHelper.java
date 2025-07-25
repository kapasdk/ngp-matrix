package org.example.matrix.chat;

import cn.hutool.extra.spring.SpringUtil;
import lombok.experimental.UtilityClass;
import org.example.matrix.services.ServiceProxyFactory;

/**
 * 聊天工具
 *
 * @author pinkman
 * @since 1.0
 */
@UtilityClass
public class ChatHelper {

    public static final ChatService service = new ServiceProxyFactory().createProxy(ChatService.class, () -> SpringUtil.getBean(ChatService.class));

}
