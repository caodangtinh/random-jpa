package com.github.kuros.random.jpa.testUtil;

import com.github.kuros.random.jpa.testUtil.entity.Shift;
import com.github.kuros.random.jpa.testUtil.entity.Department;
import com.github.kuros.random.jpa.testUtil.entity.Employee;
import com.github.kuros.random.jpa.testUtil.entity.EmployeeDepartment;
import com.github.kuros.random.jpa.testUtil.entity.Person;
import org.hibernate.jpa.HibernateEntityManager;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.mockito.Mockito;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Kumar Rohit on 4/25/15.
 */
public final class MockEntityManagerProvider {

    private EntityManager entityManager;
    private EntityManagerFactory entityManagerFactory;
    private Metamodel metamodel;

    public static MockEntityManagerProvider createMockEntityManager() {
        final MockEntityManagerProvider mockEntityManagerProvider = new MockEntityManagerProvider();
        mockEntityManagerProvider.populate();
        return mockEntityManagerProvider;
    }

    private MockEntityManagerProvider() {
        this.entityManager = Mockito.mock(HibernateEntityManager.class);
        this.entityManagerFactory = Mockito.mock(HibernateEntityManagerFactory.class);
        this.metamodel = Mockito.mock(Metamodel.class);
    }

    public void populate() {
        Mockito.when(entityManager.getEntityManagerFactory()).thenReturn(entityManagerFactory);
        Mockito.when(entityManagerFactory.getMetamodel()).thenReturn(metamodel);
        final Set<EntityType<?>> entities = getEntities();
        Mockito.when(metamodel.getEntities()).thenReturn(entities);

    }

    public Set<EntityType<?>> getEntities() {
        final Set<EntityType<?>> entities = new HashSet<EntityType<?>>();
        entities.add(createEntityType(Department.class));
        entities.add(createEntityType(Employee.class));
        entities.add(createEntityType(EmployeeDepartment.class));
        entities.add(createEntityType(Person.class));
        entities.add(createEntityType(Shift.class));
        return entities;
    }

    public EntityType createEntityType(final Class<?> clazz) {
        final EntityType entityType = Mockito.mock(EntityType.class);
        Mockito.when(entityType.getJavaType()).thenReturn(clazz);
        return entityType;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }
}
