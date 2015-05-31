package com.github.kuros.random.jpa.resolver;

import com.github.kuros.random.jpa.mapper.HierarchyGraph;
import com.github.kuros.random.jpa.mapper.Relation;
import com.github.kuros.random.jpa.metamodel.AttributeProvider;
import com.github.kuros.random.jpa.metamodel.EntityTableMapping;
import com.github.kuros.random.jpa.types.AttributeValue;
import com.github.kuros.random.jpa.types.Entity;
import com.github.kuros.random.jpa.types.Plan;
import com.github.kuros.random.jpa.util.AttributeHelper;
import com.github.kuros.random.jpa.util.NumberUtil;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Kumar Rohit on 5/17/15.
 */
public final class EntityResolverImpl implements EntityResolver {

    private final HierarchyGraph hierarchyGraph;
    private final EntityManager entityManager;
    private final Plan entityList;
    private AttributeProvider attributeProvider;

    private EntityResolverImpl(final EntityManager entityManager, final HierarchyGraph hierarchyGraph, final Plan plan) {
        this.entityList = plan;
        this.entityManager = entityManager;
        this.hierarchyGraph = hierarchyGraph;
        this.attributeProvider = AttributeProvider.getInstance(entityManager);
    }


    public static EntityResolverImpl newInstance(final EntityManager entityManager, final HierarchyGraph hierarchyGraph, final Plan plan) {
        return new EntityResolverImpl(entityManager, hierarchyGraph, plan);
    }

    @Override @SuppressWarnings("unchecked")
    public Map<Field, Object> getFieldValueMap() {
        final Map<Field, Object> fieldValue = new HashMap<Field, Object>();
        final List<Entity> entities = this.entityList.getEntities();

        for (Entity entity : entities) {
            final List<AttributeValue> attributeValues = entity.getAttributeValues();
            for (AttributeValue attributeValue : attributeValues) {
                try {
                    final Field field = AttributeHelper.getField(attributeValue.getAttribute());
                    fieldValue.put(field, attributeValue.getValue());
                    addParentDetailsForIdField(fieldValue, field);
                } catch (final Exception e) {
                    //do nothing
                }
            }
        }

        return fieldValue;
    }

    private void addParentDetailsForIdField(final Map<Field, Object> fieldValueMap, final Field field) throws IllegalAccessException {
        generateIdForParent(fieldValueMap, field);
    }

    private void generateIdForParent(final Map<Field, Object> fieldValueMap, final Field field) throws IllegalAccessException {

        final EntityTableMapping entityTableMapping = attributeProvider.get(field.getDeclaringClass());
        final Set<Relation> relations = hierarchyGraph.getAttributeRelations(field.getDeclaringClass());

        if (entityTableMapping.getAttributeIds().contains(field.getName()) || relations == null) {
            return;
        }

        final Object byId = findById(field.getDeclaringClass(), fieldValueMap.get(field));
        if (byId == null) {
            throw new IllegalArgumentException("Element not found with id: " + fieldValueMap.get(field)
                    + ", Class: " + field.getDeclaringClass());
        }

        for (Relation relation : relations) {
            final Field from = relation.getFrom().getField();
            from.setAccessible(true);
            final Object value = from.get(byId);
            final Field to = relation.getTo().getField();

            fieldValueMap.put(to, NumberUtil.castNumber(to.getType(), value));

            generateIdForParent(fieldValueMap, to);
        }
    }


    public <T> T findById(final Class<T> type, final Object value) {
        return entityManager.find(type, value);
    }

}
