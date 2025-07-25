package org.example.matrix.index;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import lombok.experimental.UtilityClass;
import org.example.matrix.fc.KMEntityConverterEx;
import org.example.matrix.fc.Persistable2;
import org.springframework.ai.document.Document;

import java.util.Map;

@UtilityClass
public class EntityToDocumentConvertor {

    public <T extends Persistable2> Document toDocument(T entity) {
        Map<String, ?> properties = KMEntityConverterEx.parseInstance(entity);

        String textTpl = "编号：{number} ；名称：{name} ；描述：{description}；";
        String text = StrUtil.format(textTpl, properties, false);

        Map<String, Object> metadata = MapUtil.newHashMap();
        metadata.putAll(properties);
        metadata.entrySet().removeIf(entry -> ObjUtil.isNull(entry.getValue()));

        String documentId = entity.getIdentityInfo().getUUIDformat();
        return Document.builder().id(documentId).text(text).metadata(metadata).build();
    }

    public <T extends Persistable2> T toEntity(Document document) {
        return null;
    }

}
