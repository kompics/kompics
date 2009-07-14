package se.sics.kompics.wan.plab;

import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

@Repository
public class PLabHostDaoImpl implements PLabHostDao {

	private SessionFactory sessionFactory = null;

	
	public PLabHostDaoImpl() {
		// default constructor for hibernate
	}

	public PLabHostDaoImpl(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
//		this.sessionFactory = new AnnotationConfiguration().buildSessionFactory();
	}

	// private HibernateTemplate ht = null;
	// private static final SessionFactory sessionFactory;

	// static {
	// try { // Create the SessionFactory from hibernate.cfg.xml
	// sessionFactory = new Configuration().configure().buildSessionFactory();
	// } catch (Throwable ex) {
	// // Make sure you log the exception, as it might be swallowed
	// System.err.println("Initial SessionFactory creation failed." + ex);
	// throw new ExceptionInInitializerError(ex);
	// }
	// }

	 public SessionFactory getSessionFactory() {
	 return this.sessionFactory;
	 }
	
	 public void setSessionFactory(SessionFactory sessionFactory) {
	 this.sessionFactory = sessionFactory;
	 // ht = new HibernateTemplate(sessionFactory);
	 }

	// @SuppressWarnings("unchecked")
	// @Override
	// public List<PLabHost> loadAllHosts() {
	// // Session session = sessionFactory.openSession();
	// // List<PLabHost> list = session.createQuery("From PLabHost").list();
	//
	// List<PLabHost> list = (List<PLabHost>) ht.find("from PLabHost");
	// return list;
	//
	// }
	//
	// @Override
	// public void store(List<PLabHost> listHosts) {
	//
	// // Session session = sessionFactory.openSession();
	// // session.getTransaction().begin();
	//
	// for (PLabHost host : listHosts) {
	// // session.save(host);
	// ht.save(host);
	// }
	// // session.getTransaction().commit();
	//
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.wan.master.plab.PLabHostDao#loadPLabStore()
	 */
	@Override
	public PLabStore load(String slice) {
//		Transaction tx = null;
//		Session session = sessionFactory.getCurrentSession();
//		tx = session.beginTransaction();
//		PLabStore store = (PLabStore) session.createQuery(
//				"from PLabStore where slice=?").setParameter(0, slice);
//		tx.commit();
//		return store;
//		sessionFactory.getCurrentSession().beginTransaction();

//		PLabStore store = (PLabStore) sessionFactory.getCurrentSession().load(PLabStore.class, slice);
		PLabStore store = (PLabStore) sessionFactory.getCurrentSession().get(PLabStore.class, slice);

		return store;
//		Query query = sessionFactory.getCurrentSession().createQuery(
//				"from PLabStore where slice=?").setParameter(0, slice);
//	    List<PLabStore> events =query.list();
//	    return events.get(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * se.sics.kompics.wan.master.plab.PLabHostDao#store(se.sics.kompics.wan
	 * .master.plab.PLabStore)
	 */
	@Override
	public void save(PLabStore store) {
		this.sessionFactory.getCurrentSession().save(store);
	}

}
