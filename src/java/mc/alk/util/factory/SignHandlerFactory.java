package mc.alk.util.factory;

import mc.alk.util.handlers.ISignHandler;

public class SignHandlerFactory {

    private static HandlerFactory<ISignHandler> factory = new HandlerFactory<ISignHandler>();

    public static ISignHandler getNewInstance() {
        ISignHandler handler = factory.getNewInstance("SignHandler");
        return (handler == null) ? ISignHandler.NULL_HANDLER : handler;
    }

}
