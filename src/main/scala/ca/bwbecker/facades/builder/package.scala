package ca.bwbecker.facades

/**
  * Created by bwbecker on 2017-05-25.
  */
package object builder {

  /**
    * A map of option values, which JSOptionBuilder builds up.
    */
  type OptMap = Map[String, Any]


  /**
    * An initial empty map of option values, which you use to begin building up
    * the options object.
    */
  val noOpts = Map.empty[String, Any]

}
