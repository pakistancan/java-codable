/**
 * 
 */
package io.github.pakistancan.codable.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author muhammadali
 * 
 *         Generates Codable, also generate reference classes that are
 *         referenced in the class if they are in the same package
 */

@Inherited
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Codable {

}
