package at.htl.jpa.locking.db;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class DbUtil {

    private static EntityManagerFactory emf;
    private static EntityManager em;

    private DbUtil() { }

    public static EntityManager getEntityManager() {

        if (emf == null) {
            emf = Persistence.createEntityManagerFactory("myPU");
        }

        if (em == null || !em.isOpen()) {
            em = emf.createEntityManager();
        }

        return em;
    }

    public static void shutdown() {
        if (em != null) {
            em.close();
        }

//        if (emf != null) {
//            emf.close();
//        }
    }
}
