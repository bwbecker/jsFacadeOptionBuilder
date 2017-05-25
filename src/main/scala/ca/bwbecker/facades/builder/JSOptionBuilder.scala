package ca.bwbecker.facades.builder

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

/**
  * Trait enabling hierarchical option structures where options can
  * be inherited from other classes.
  *
  * @tparam T See [[JSOptionBuilder]]
  * @tparam B See [[JSOptionBuilder]]
  */
trait JSOptionSetter[T <: js.Object, B <: JSOptionBuilder[T, _]] {
  protected def jsOpt(name: String, opt: Any): B
}

/**
 * Helper class for defining strongly-typed "options" classes to pass into Scala.js facades.
 * This approach is particularly helpful for jQuery-based facades, which often take very
 * complex options objects, with large numbers of polymorphic fields.
 *
 * @tparam T A placeholder facade trait -- usually just a declaration of a trait that inherits from js.Object.
 * @tparam B This class. (It is probably possible to eliminate this declaration, but I haven't figured it out yet.)
 */
abstract class JSOptionBuilder[T <: js.Object, B <: JSOptionBuilder[T, _]] extends JSOptionSetter[T, B] {


  /**
    * In a class X that extends JSOptionBuilder, implement the boilerplate
    *
    * {{{
    * def copy(newDictionary: OptMap): X = {
    *    new X(newDictionary)
    * }
    * }}}
    *
    * The original version of JSOptionBuilder passed this as a class parameter.  However,
    * IntelliJ refused to do autocompletion with that version and seemed to consume lots of CPU.
    * Doing it this way works.
    */
  def copy(newDictionary: OptMap): B

  /**
   * This is a dictionary of option values. It is usually *very* heterogeneous,
   * mixing everything from Ints to Functions. So it needs to be js.Any.
   */
  protected def dict:OptMap

  /**
   * Define one field in an options class.
   *
   * Note that jsOpt is not, in and of itself, strongly-typed. You use this helper to
   * add a strongly-typed method for each field.
   */
  override protected def jsOpt(name:String, opt:Any):B = {
    copy(dict + (name -> opt))
  }

  /**
   * Extract the built-up options, in a form suitable for passing into a typical facade.
   */
  private def _result = {
    dict.toJSDictionary.asInstanceOf[T]
  }

  override def toString = {
    s"""{\n${dict.keys.map{ key => s"  $key = ${dict(key).toString}"}.mkString("\n")}\n}"""
  }
}

object JSOptionBuilder {

  /**
   * Automatically extract the result from a JSOptionBuilder when necessary.
   */
  implicit def builder2Options[T <: js.Object](builder: JSOptionBuilder[T,_]): T = builder._result

}
