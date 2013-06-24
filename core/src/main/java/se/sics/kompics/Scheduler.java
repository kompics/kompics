package se.sics.kompics;

public abstract class Scheduler {

    public abstract void schedule(Component c, int w);

    public abstract void proceed();

    public abstract void shutdown();

    public abstract void asyncShutdown();

    protected final void executeComponent(Component component, int w) {
        //Kompics.logger.error("Executing: {}", component.getComponent());
        ((ComponentCore) component).execute(w);
        //Kompics.logger.error("Finished executing: {}", component.getComponent());
    }
}
