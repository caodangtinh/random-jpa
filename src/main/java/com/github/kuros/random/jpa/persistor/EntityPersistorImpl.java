package com.github.kuros.random.jpa.persistor;

import com.github.kuros.random.jpa.definition.TableNode;
import com.github.kuros.random.jpa.exception.RandomJPAException;
import com.github.kuros.random.jpa.log.LogFactory;
import com.github.kuros.random.jpa.log.Logger;
import com.github.kuros.random.jpa.mapper.Relation;
import com.github.kuros.random.jpa.metamodel.AttributeProvider;
import com.github.kuros.random.jpa.metamodel.model.EntityTableMapping;
import com.github.kuros.random.jpa.persistor.model.ResultMap;
import com.github.kuros.random.jpa.persistor.model.ResultMapImpl;
import com.github.kuros.random.jpa.provider.UniqueConstraintProvider;
import com.github.kuros.random.jpa.provider.UniqueConstraintProviderFactory;
import com.github.kuros.random.jpa.types.CreationOrder;
import com.github.kuros.random.jpa.types.CreationPlan;
import com.github.kuros.random.jpa.types.Node;
import com.github.kuros.random.jpa.util.NumberUtil;
import com.github.kuros.random.jpa.util.Util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/*
 * Copyright (c) 2015 Kumar Rohit
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License or any
 *    later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public final class EntityPersistorImpl implements Persistor {

    private static final Logger LOGGER = LogFactory.getLogger(EntityPersistorImpl.class);

    private EntityManager entityManager;
    private AttributeProvider attributeProvider;
    private UniqueConstraintProvider uniqueConstraintProvider;

    private EntityPersistorImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
        this.attributeProvider = AttributeProvider.getInstance();
        this.uniqueConstraintProvider = UniqueConstraintProviderFactory.getUniqueConstraintProvider();
    }

    public static Persistor newInstance(final EntityManager entityManager) {
        return new EntityPersistorImpl(entityManager);
    }

    @SuppressWarnings("unchecked")
    public ResultMap persist(final CreationPlan creationPlan) {
        final Node root = Node.newInstance();
        final ResultMapImpl resultMap = ResultMapImpl.newInstance(root);


        final Node creationPlanRoot = creationPlan.getRoot();
        final List<Node> childNodes = creationPlanRoot.getChildNodes();
        for (Node node : childNodes) {
            if (node.getValue() != null) {
                final Node childNode = Node.newInstance(node.getType(), getIndex(resultMap, node.getType()));
                root.addChildNode(childNode);
                persist(childNode, creationPlan.getCreationOrder(), resultMap, node);
            }
        }

        return resultMap;
    }

    private int getIndex(final ResultMapImpl resultMap, final Class type) {
        final List<Object> objects = resultMap.getCreatedEntities().get(type);
        return isEmpty(objects) ? 0 : objects.size();
    }

    private boolean isEmpty(final List<Object> objects) {
        return objects == null || objects.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private void persist(final Node resultNode, final CreationOrder creationOrder, final ResultMapImpl resultMap, final Node node) {
        final Object random = createRandomObject(node, creationOrder, resultMap);
        Object persistedObject;
        if (getId(node.getType(), random) != null
                && findElementById(node.getType(), random) != null) {
            persistedObject = findElementById(node.getType(), random);
        } else {
            final Object foundRow = findRowByUniqueIdentities(node.getType(), random);
            persistedObject = foundRow != null ? foundRow : persistAndReturnPersistedObject(node.getType(), random);
        }

        resultMap.put(node.getType(), persistedObject);

        final List<Node> childNodes = node.getChildNodes();
        for (Node childNode : childNodes) {
            if (childNode.getValue() != null) {
                final Node resultChildNode = Node.newInstance(childNode.getType(), getIndex(resultMap, childNode.getType()));
                resultNode.addChildNode(resultChildNode);
                persist(resultChildNode, creationOrder, resultMap, childNode);
            }
        }

    }

    private Object persistAndReturnPersistedObject(final Class tableClass, final Object random) {
        try {
            final EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();
            final EntityManager em = entityManagerFactory.createEntityManager();
            em.getTransaction().begin();
            em.persist(random);
            em.getTransaction().commit();
            em.close();
            LOGGER.debug("Persisted values for table: " + tableClass.getName());
        } catch (final Exception e) {
            LOGGER.error("Failed to persist: " + tableClass.getName());
            throw new RandomJPAException(e);
        }
        return findElementById(tableClass, random);
    }

    @SuppressWarnings("unchecked")
    private Object findRowByUniqueIdentities(final Class tableClass, final Object random) {
        final List<String> uniqueCombinationAttributes = uniqueConstraintProvider.getUniqueCombinationAttributes(tableClass);
        if (uniqueCombinationAttributes == null || uniqueCombinationAttributes.size() <= 1) {
            return null;
        }

        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery q = criteriaBuilder.createQuery(tableClass);

        final Root<?> from = q.from(tableClass);

        q.select(from);

        final Predicate[] predicates = new Predicate[uniqueCombinationAttributes.size()];

        for (int i = 0; i < uniqueCombinationAttributes.size(); i++) {
            final String attribute = uniqueCombinationAttributes.get(i);
            try {
                final Field declaredField = tableClass.getDeclaredField(attribute);
                declaredField.setAccessible(true);
                predicates[i] = criteriaBuilder.equal(from.get(attribute), declaredField.get(random));
            } catch (final Exception e) {
                throw new RandomJPAException(e);
            }
        }

        q.where(predicates);

        final TypedQuery typedQuery = entityManager.createQuery(q);
        final List resultList = typedQuery.getResultList();

        if (resultList.size() > 0) {
            LOGGER.debug("Reusing data for: " + tableClass + " " + Util.printValues(random));
        }

        return resultList.size() == 0 ? null : resultList.get(0);
    }

    private Object findElementById(final Class tableClass, final Object persistedObject) {
        return entityManager.find(tableClass, getId(tableClass, persistedObject));
    }

    private Object getId(final Class tableClass, final Object persistedObject) {
        final EntityTableMapping entityTableMapping = attributeProvider.get(tableClass);
        final Field[] declaredFields = tableClass.getDeclaredFields();
        Field field = null;
        for (Field declaredField : declaredFields) {
            if (entityTableMapping.getAttributeIds().contains(declaredField.getName())) {
                field = declaredField;
                field.setAccessible(true);
                break;
            }
        }

        Object id = null;
        try {
            id = field != null ? field.get(persistedObject) : null;
        } catch (final IllegalAccessException e) {
            //do nothing
        }
        return id;
    }


    private Object createRandomObject(final Node node, final CreationOrder creationOrder, final ResultMapImpl resultMap) {
        final Object random = node.getValue();

        final TableNode tableNode = creationOrder.getTableNode(node.getType());
        final List<Relation> relations = tableNode.getRelations();

        for (Relation relation : relations) {
            createRelation(resultMap, relation, random);
        }


        return random;
    }

    private void createRelation(final ResultMapImpl resultMap, final Relation relation, final Object object) {
        try {
            final Object value = getFieldValue(resultMap, relation.getTo());
            setFieldValue(object, relation.getFrom(), value);
        } catch (final Exception e) {
            //do nothing
        }

    }

    private void setFieldValue(final Object object, final Field field, final Object value) {
        try {

            final Class<?> type = field.getType();
            field.setAccessible(true);
            field.set(object, NumberUtil.castNumber(type, value));
        } catch (final IllegalAccessException e) {
            //do nothing
        }
    }



    private Object getFieldValue(final ResultMapImpl resultMap, final Field field) {
        final Map<Class<?>, List<Object>> createdEntities = resultMap.getCreatedEntities();
        final List<Object> objects = createdEntities.get(field.getDeclaringClass());
        final Object object = objects.get(objects.size() - 1);
        Object value = null;
        try {
            field.setAccessible(true);
            value = field.get(object);
        } catch (final IllegalAccessException e) {
            //do nothing
        }
        return value;
    }
}
