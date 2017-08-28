package tigase.ui;

/**
 * Objects which implements this interface can interract with user through some interface.
 * This can be WWW interface or some standalone application.
 * Everything it needs to do is to return set of possible parameters which can be modified and then passed to UIComponent implementation to process.
 * Some of parameters can be read-only lika work statistics other can be changed like configuration settings or user DB.
 */
public interface UIComponent {
}
