package com.github.kuros.random.jpa.provider.factory;

import com.github.kuros.random.jpa.Database;
import com.github.kuros.random.jpa.metamodel.AttributeProvider;
import com.github.kuros.random.jpa.provider.UniqueConstraintProvider;
import com.github.kuros.random.jpa.provider.h2.H2UniqueConstraintProvider;
import com.github.kuros.random.jpa.provider.mssql.MSSQLUniqueConstraintProvider;
import com.github.kuros.random.jpa.provider.mysql.MySqlUniqueConstraintProvider;
import com.github.kuros.random.jpa.provider.oracle.OracleUniqueConstraintProvider;
import com.github.kuros.random.jpa.provider.postgres.PostgresUniqueConstraintProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UniqueConstraintProviderFactoryTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;
    @Mock
    private AttributeProvider attributeProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.when(entityManager.createNativeQuery(Mockito.anyString())).thenReturn(query);
    }

    @Test
    public void getUniqueCombinationAttributesForMsSqlServer() {
        final UniqueConstraintProvider uniqueConstraintProvider = UniqueConstraintProviderFactory.getUniqueConstraintProvider(Database.MS_SQL_SERVER, entityManager, attributeProvider);
        assertTrue(uniqueConstraintProvider instanceof MSSQLUniqueConstraintProvider);
    }

    @Test
    public void getUniqueCombinationAttributesForMySql() {
        final UniqueConstraintProvider uniqueConstraintProvider = UniqueConstraintProviderFactory.getUniqueConstraintProvider(Database.MY_SQL, entityManager, attributeProvider);
        assertTrue(uniqueConstraintProvider instanceof MySqlUniqueConstraintProvider);
    }

    @Test
    public void getUniqueCombinationAttributesForOracle() {
        final UniqueConstraintProvider uniqueConstraintProvider = UniqueConstraintProviderFactory.getUniqueConstraintProvider(Database.ORACLE, entityManager, attributeProvider);
        assertTrue(uniqueConstraintProvider instanceof OracleUniqueConstraintProvider);
    }

    @Test
    public void getUniqueCombinationAttributesForPostgres() {
        final UniqueConstraintProvider uniqueConstraintProvider = UniqueConstraintProviderFactory.getUniqueConstraintProvider(Database.POSTGRES, entityManager, attributeProvider);
        assertTrue(uniqueConstraintProvider instanceof PostgresUniqueConstraintProvider);
    }

    @Test
    public void getUniqueCombinationAttributesForH2() {
        final UniqueConstraintProvider uniqueConstraintProvider = UniqueConstraintProviderFactory.getUniqueConstraintProvider(Database.H2, entityManager, attributeProvider);
        assertTrue(uniqueConstraintProvider instanceof H2UniqueConstraintProvider);
    }

    @Test
    public void getUniqueCombinationAttributesForNoDatabase() {
        final UniqueConstraintProvider uniqueConstraintProvider = UniqueConstraintProviderFactory.getUniqueConstraintProvider(Database.NONE, entityManager, attributeProvider);
        assertTrue(uniqueConstraintProvider instanceof UniqueConstraintProviderFactory.DefaultUniqueConstraintProvider);

        assertEquals(0, uniqueConstraintProvider.getUniqueCombinationAttributes(Class.class).size());
    }
}
