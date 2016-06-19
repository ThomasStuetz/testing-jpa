package at.htl.jpa.locking;

import at.htl.jpa.locking.db.DbUtil;
import at.htl.jpa.locking.entity.Product;
import at.htl.jpa.locking.entity.ProductWithoutVersion;
import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.MethodSorters;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.List;

import static org.eclipse.persistence.jpa.jpql.Assert.fail;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MainTest {

    private EntityManager em;

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println("\n\n----------------------------------------");
            System.out.println("*** Starting test: " + description.getMethodName());
        }
    };

    @Before
    public void setUp() throws Exception {
        em = DbUtil.getEntityManager();
        em.getTransaction().begin();
        em.createNativeQuery("delete from PRODUCT").executeUpdate();
        em.createNativeQuery("delete from PRODUCT_WO_VERS").executeUpdate();
        em.getTransaction().commit();
    }

    @After
    public void tearDown() throws Exception {
        DbUtil.shutdown();
    }

    @Test
    public void t001persistProduct() {

        Product milk = new Product("Milch", 1.0f);

        assertThat("Id is supposed to be NULL", milk.getId(), is(nullValue()));

        showTable("Product");

        em.getTransaction().begin();
        milk = em.merge(milk);
        em.getTransaction().commit();

        assertThat(milk.getId(), is(notNullValue()));

        showTable("Product");
    }

    @Test
    public void t002ProductTransactionsSerialized() {

        EntityTransaction t1 = em.getTransaction();
        EntityTransaction t2 = em.getTransaction();

        t1.begin();
        Product milk = em.merge(new Product("Milch", 1.5f));
        t1.commit();

        showTable("Product");

        t1.begin();
        Product milkReadT1 = em.find(Product.class, milk.getId());
        milkReadT1.setPrice(2.0f);
        t1.commit();

        try {
            t2.begin();
            Product milkReadT2 = em.find(Product.class, milk.getId());
            milkReadT2.setDescription(milkReadT2.getDescription() + " T2");
            t2.commit();
        } catch (Exception e) {
            System.err.println("T2 aborted: " + e.getMessage());
        }

        showTable("Product");
    }

    /**
     * In this test the EntityManager takes care of the Locking
     */
    @Test
    public void t003ProductTransactionsOverlapping() {

        EntityTransaction t1 = em.getTransaction();
        EntityTransaction t2 = em.getTransaction();

        t1.begin();
        Product milk = em.merge(new Product("Milch", 1.5f));
        t1.commit();

        showTable("Product");

        t1.begin();
        Product milkReadT1 = em.find(Product.class, milk.getId());
        milkReadT1.setPrice(2.0f);

        try {
            t2.begin();
            Product milkReadT2 = em.find(Product.class, milk.getId());
            milkReadT2.setDescription(milkReadT2.getDescription() + " T2");
            t2.commit();
        } catch (Exception e) {
            System.err.println("T2 aborted: " + e.getMessage());
        }

        t1.commit();
        showTable("Product");
    }

    @Test //(expected = javax.persistence.OptimisticLockException.class)
    public void t010ProductStaleObjectState() {

        EntityTransaction t1 = em.getTransaction();
        EntityTransaction t2 = em.getTransaction();

        t1.begin();
        Product milk = em.merge(new Product("Milch", 1.5f));
        t1.commit();

        showTable("Product");

        // Transaction 1 (beginTransaction not needed because of read operation)
        // read object
        Product milkReadT1 = em.find(Product.class, milk.getId());

        // detach object
        em.detach(milkReadT1);

        // change value of object
        milkReadT1.setPrice(2.0f);


        // Transaction 2
        try {
            t2.begin();
            Product milkReadT2 = em.find(Product.class, milk.getId());
            milkReadT2.setDescription(milkReadT2.getDescription() + " T2");
            t2.commit();
        } catch (Exception e) {
            System.err.println("T2 aborted: " + e.getMessage());
        }

        // Transaction 1
        // write changes
        try {
            t1.begin();
            em.merge(milkReadT1);
            t1.commit();
            fail("OptimisticLockException should have thrown");
        } catch (Exception e) {
            assertThat(e, instanceOf(javax.persistence.OptimisticLockException.class));
            System.out.println("T1: OptimisticLockException thrown: change of price not allowed");
        }
        List<Product> products = (List<Product>) showTable("Product");
        assertThat("T1 should not have worked", products.get(0).getPrice(), is(1.5f));
    }

    @Test //(expected = javax.persistence.OptimisticLockException.class)
    public void t015ProductWithoutVersionStaleObjectState() {

        EntityTransaction t1 = em.getTransaction();
        EntityTransaction t2 = em.getTransaction();

        t1.begin();
        ProductWithoutVersion milk = em.merge(new ProductWithoutVersion("Milch", 1.5f));
        t1.commit();

        System.out.println("Initial state");
        showTable("ProductWithoutVersion");

        // Transaction 1 (beginTransaction not needed because of read operation)
        // read object
        ProductWithoutVersion milkReadT1 = em.find(ProductWithoutVersion.class, milk.getId());

        // detach object
        em.detach(milkReadT1);

        // change value of object
        milkReadT1.setPrice(2.0f);


        // Transaction 2
        try {
            t2.begin();
            ProductWithoutVersion milkReadT2 = em.find(ProductWithoutVersion.class, milk.getId());
            milkReadT2.setDescription(milkReadT2.getDescription() + " T2");
            t2.commit();
        } catch (Exception e) {
            System.err.println("T2 aborted: " + e.getMessage());
//            e.printStackTrace();
        }
        System.out.println("T2 changed Description:");
        showTable("ProductWithoutVersion");

        // Transaction 1
        // write changes
        t1.begin();
        em.merge(milkReadT1);
        t1.commit();
        System.out.println("LOST UPDATE: the changes of T2 are lost");
        showTable("ProductWithoutVersion");
    }


    private List<?> showTable(String tableName) {

        Class tableType = null;
        try {
            tableType = Class.forName(tableName);
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
        }

        List<?> tableContent = em.createQuery("select p from " + tableName + " p", tableType)
                .getResultList();

        System.out.println(tableContent);
        return tableContent;
    }

}