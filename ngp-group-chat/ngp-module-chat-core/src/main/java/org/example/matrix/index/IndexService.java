package org.example.matrix.index;

import org.example.matrix.fc.Persistable2;
import org.example.matrix.services.RemoteInterface;
import org.springframework.ai.document.Document;

import java.util.List;

@RemoteInterface
public interface IndexService {

    /**
     * 初始化向量库
     *
     * @param clazz 实体类
     */
    <T extends Persistable2> void build(Class<T> clazz);

    /**
     * 重新初始化向量库
     *
     * @param clazz 实体类
     */
    <T extends Persistable2> void rebuild(Class<T> clazz);

    /**
     * 建立索引
     *
     * @param entity 实体
     * @return 文档
     */
    <T extends Persistable2> Document index(T entity);

    /**
     * 建立索引
     *
     * @param entities 实体
     * @return 文档
     */
    <T extends Persistable2> List<Document> index(List<? extends T> entities);

    /**
     * 移除索引
     *
     * @param entity 实体
     */
    <T extends Persistable2> void deindex(T entity);

    /**
     * 移除索引
     *
     * @param entities 实体
     */
    <T extends Persistable2> void deindex(List<? extends T> entities);

    /**
     * 是否被索引
     *
     * @param entity 实体
     */
    <T extends Persistable2> boolean indexed(T entity);

    /**
     * 搜索（相似对象，按照相似度排序）
     *
     * @param clazz 实体类
     * @param query 对话，自然语言
     */
    <T extends Persistable2> List<T> search(Class<T> clazz, String query);

    /**
     * 搜索（相似对象，按照相似度排序）
     *
     * @param clazz 实体类
     * @param query 对话，自然语言
     * @param limit 返回结果个数
     */
    <T extends Persistable2> List<T> search(Class<T> clazz, String query, long limit);

}
