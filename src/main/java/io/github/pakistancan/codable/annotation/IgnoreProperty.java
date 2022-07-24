package io.github.pakistancan.codable.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
/**
 * 
 * @author muhammadali
 * 
 *         Ignores property and don't generate a mapping for the given property
 *
 */
public @interface IgnoreProperty {

}