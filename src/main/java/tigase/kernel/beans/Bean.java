package tigase.kernel.beans;

import java.lang.annotation.*;

/**
 * Defines name of bean.
 * <p>
 * This annotation is not required, but each bean must be named! Instead of
 * using annotation, name of bean may be defined during registration.
 * </p>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Bean {

	/**
	 * Name of bean.
	 * 
	 * @return name of bean.
	 */
	String name();

	/**
	 * Is active by default
	 * @return
	 */
	boolean active();

	boolean exportable() default false;

	Class parent() default Object.class;

	Class[] parents() default {};

	Class<? extends BeanSelector>[] selectors() default { };

}