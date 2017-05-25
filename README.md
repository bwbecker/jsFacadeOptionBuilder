# jsFacadeOptionBuilder
A useful tool for building Scala.js facades.

This is a fork from https://github.com/jducoeur/jsext but involves a
breaking change.  Why?  Facades developed with the original and put
into a library do not work with IntelliJ's autocomplete for some reason
and seem to raise IntelliJ's CPU usage dramatically.  Note that this
only occurs when using a library built with the original tool, not when
the code is included directly in an IntelliJ project.

I also removed some features that were unrelated to building Scala.js
facades.

## Installation

To use jsext, add this line to your Scala.js project's libraryDependencies:
```scala
"ca.bwbecker" %%% "jsFacadeOptionBuilder" % "0.8"
```


## JSOptionBuilder

JSOptionBuilder is designed to deal with a common problem in building facades, especially facades of JQuery widgets.

A typical such facade is usually initialized with an "options" object that defines its exact behavior. The problem is
that these options objects often contain many fields -- 20-40 fields are not unusual -- some of which are polymorphic in a way that is hard to
represent in Scala. (Since JavaScript is loosely typed, you can have polymorphic unions simply by accepting several different types for a parameter, and
testing what actually got passed in to figure out the type.) The result is a combinatoric explosion, where a single options object could potentially
require hundreds, even thousands of strongly-typed constructors in order to properly represent it.

There are several potential ways to deal with this problem. In JSOptionBuilder, we choose not to even try to define a Scala constructor for the options
object. Instead, we build the options object up through a series of chained function calls. That tames the polymorphism, since each field can be a
separate overloaded function.

### Defining a JSOptionBuilder

JSOptionBuilder is easiest to understand with a worked-out example. In this case, we are going to use jQuery UI's Dialog widget, which has about 30
fields. (You can find the full definition of this Widget [in the jQuery UI API documentation](http://api.jqueryui.com/dialog/). The facade of the widget
itself is quite simple:
```scala
@js.native
trait JQueryUIDialogFacade extends js.Object {
  def dialog(options:DialogOptions):JQuery = js.native
}
```
That is, this says that the referenced jQuery object should be considered a Dialog, using the specified DialogOptions. All the work comes in defining
DialogOptions.

To begin with, we provide three related definitions:
```scala
@js.native
trait DialogOptions extends js.Object
object DialogOptions extends DialogOptionBuilder(noOpts)
class DialogOptionBuilder(val dict:OptMap)
  extends JSOptionBuilder[DialogOptions, DialogOptionBuilder] {
    def copy(nd:OptMap):DialogOptionBuilder = new DialogOptionBuilder(nd)
}
```
The `DialogOptions` trait is what we're trying to produce: a facade for the options object that we'll pass into the `dialog()` function above.

The `DialogOptions` *companion object* is an empty DialogOptions. It is legal to pass into `dialog()` (after the implicit conversions we will talk
about later), but is nothing but defaults.

All the interesting stuff goes into the `DialogOptionBuilder` class. Into this, we put a bunch of declarations, one for each overload of each
field, like these:
```scala
  def appendTo(v:String) = jsOpt("appendTo", v)

  def autoOpen(v:Boolean) = jsOpt("autoOpen", v)

  def buttons(v:js.Dictionary[js.Function0[Any]]) = jsOpt("buttons", v)
  def buttons(v:js.Array[js.Object]) = jsOpt("buttons", v)
```
That is, for each field, we define a method that takes the type that is legal to pass into that field. We pass that value into jsOpt(),
which returns a new DialogOptionBuilder with that value added -- everything is immutable, and you chain these calls together to get the
fully-constructed options object. If a field accepts multiple types, then you define one overload of the function for each type.

Note that the JSOptionBuilder companion object includes an implicit def, which converts from the Builder to the target trait.

### Defining a JSOptionBuilder with inheritance

For hierarchical option structures you can inherit options from other classes.
Then you have to split out all `jsOpt` calls into traits and write:
```scala
@ScalaJSDefined
trait DialogOptions extends WidgetOptions
object DialogOptions extends DialogOptionsBuilder(noOpts)
class DialogOptionsBuilder(val dict: OptMap)
  extends JSOptionsBuilder[DialogOptions, DialogOptionsBuilder]
    with DialogSetters[DialogOptions, DialogOptionsBuilder] {
      def copy(nd:OptMap):DialogOptionsBuilder = new DialogOptionsBuilder(nd)
}
trait DialogSetters[T <: js.Object, B <: JSOptionBuilder[T,_]]
  extends WidgetSetters[T, B] {
    def title(v: String) = jsOpt("title", v)
}

@ScalaJSDefined
trait WidgetOptions extends js.Object
object WidgetOptions extends WidgetOptionsBulder(noOpts)
class WidgetOptionsBuilder(val dict: OptMap)
  extends JSOptionsBuilder[WidgetOptions, WidgetOptionsBuilder]
    with WidgetSetters[WidgetOptions, WidgetOptionsBuilder] {
      def copy(nd:OptMap):WidgetOptionsBuilder = new WidgetOptionsBuilder(nd)
}
trait WidgetSetters[T <: js.Object, B <: JSOptionBuilder[T,_]]
  extends JSOptionSetter[T, B] {
    def height(v: Int) = jsOpt("height", v)
}
```
Now both `title` and `height` are available for `DialogOptions`, but for
`WidgetOptions` only `height` is available.

### Using the JSOptionBuilder

Using the resulting class is done with chained function calls instead of a constructor, but the number of characters you actually type
is roughly the same. A typical call looks like this:
```scala
val asDialog = $(elem).dialog(DialogOptions.
  title(dialogTitle).
  height(height).width(width).
  buttons(buttonMap)
)
```
That is, we start with the `DialogOptions` object defined above (which, remember, is the completely empty default options). We
call the functions for the fields we want to set, chaining them together. The implicit def detects that we are passing this into
`dialog()`, so it converts it from DialogOptionBuilder into DialogOptions.

The resulting code is reasonably concise, and strongly-typed: in good Scala fashion, type errors will be usually be caught in the IDE.

### Summary

If you are building a facade called Foo that takes an options object, you would usually define the following code:
```scala
@js.native
trait FooFacade extends js.Object {
  def foo(options:FooOptions):JQuery = js.native
}
@js.native
trait FooOptions extends js.Object
object FooOptions extends FooOptionBuilder(noOpts)
class FooOptionBuilder(val dict:OptMap)
  extends JSOptionBuilder[FooOptions, FooOptionBuilder] {
  def copy(nd:OptMap):FooOptionBuilder = new FooOptionBuilder(nd)

  def field1(v:someType) = jsOpt("field1", v)

  def field2(v:someType) = jsOpt("field2", v)
  def field2(v:someOtherType) = jsOpt("field2", v)

  // ... one jsOpt for each overload of each field
}
```
That's pretty much it. There's a little boilerplate, but not too much, and the resulting facade works well.

Obviously, the details may vary -- use common sense when applying this pattern. But it works as described for a large
number of jQuery widgets.

### JSOptionBuilder vs. ScalaJSDefined

As of Scala.js release 0.6.5, there is [a `@ScalaJSDefined` annotation](http://www.scala-js.org/doc/sjs-defined-js-classes.html), which lets you create JavaScript classes from Scala.js. In principle, this seems like it should obviate the need for JSOptionBuilder. In practice, there are some tradeoffs.

For simpler facades, and especially when you just need to create a JavaScript object with a few fields that always need to be filled in, `@ScalaJSDefined` is probably the best way to go: it lets you define the structure of the JS object with relatively little boilerplate, and the same trait or class can be used for both creating those objects and (if needed) reading native-JS versions of them. It's nice and easy.

However, for jQuery-style configuration objects, `@ScalaJSDefined` works particularly poorly, for several reasons. Remember that these config objects typically contain a relatively large number of fields, all of which are optional. For this reason, you don't want to use a `@ScalaJSDefined` trait to define one, because then you would have to fill in *all* of the fields at the call site every time you called it. (Note that these traits differ from conventional Scala traits in that they have to be 'pure' -- they can't define default values.)

A `@ScalaJSDefined` class works better for this, since it does allow you to provide defaults. But remember that all of the fields are optional. This means that they all have to be defined as `UndefOr[T]`, instead of their simple types. And you have a tradeoff to make, in how you declare the fields vs. how you fill them in. If you want to go pure-functional, you need a bit more boilerplate at the call site than I prefer, like this (making up a JQuery-style config object similar to the example in the Scala.js documentation; for sake of argument, say the default position is (0,0)):
```scala
@ScalaJSDefined
class PositionConfig extends js.Object
{
  val x:UndefOr[Int] = undefined
  val y:UndefOr[Int] = undefined
}
...
val positionedThing = $(myThing).withPosition {
  override val x = 100
}
```
To get rid of having to say `override val` for every field, you have to sacrifice purity:
```scala
@ScalaJSDefined
class PositionConfig extends js.Object
{
  var x:UndefOr[Int] = undefined
  var y:UndefOr[Int] = undefined
}
...
val positionedThing = $(myThing).withPosition {
  x = 100
}
```
That is, by using `var` instead of `val`, we get a more concise call-site syntax at the risk of using mutable fields.

Also, remember that it is very common for jQuery options to be Union types. JSOptionBuilder lets you deal with this by adding overloaded setters: you just define separate entry points for the different types. `@ScalaJSDefined` doesn't allow that: you can only have one entry point with a given name. Now, Scala.js 0.6.5 allows you to define union types with the new `js.|` operator, and that often works quite nicely. But `|` doesn't always play nicely with `UndefOf`: getting the signature correct, so that the field can contain any of several types *or* undefined, can be tricky. (In particular, when the parameter is a function type, you can get beyond the capabilities of Scala's type inferencer, and find that you have to put in excess type ascriptions at the call site.)

The final problem doesn't arise in every situation, but is insuperable when it does. jQuery has a core function named `$.isPlainObject`, which it uses to distinguish between DOM nodes and "normal" JavaScript objects. By its definition, `@ScalaJSDefined` objects are *not* "plain". The result is somewhat unpredictable behavior -- some jQuery libraries simply won't work if you pass in `@ScalaJSDefined` parameters. (*As of Scala.js 0.6.9, I believe this last point is no longer true, but I haven't tried it out myself yet.*)

So this isn't a slam-dunk argument either way. `@ScalaJSDefined` easier to set up, and can be used to for interpreting native JS objects as well as creating them, but is either wordier at the call site (if you use the pure version), or requires that you use mutable fields, and you have to be very careful with your signature definitions. And it won't always work for jQuery facades. It's up to you, as the facade designer, to decide whether you prefer that, or the extra design-side boilerplate required by JSOptionBuilder.

### TO DO

Some of the boilerplate involved in using JSOptionBuilder could probably be tamed with a few macros.

### License

Copyright (c) 2015 Querki Inc. (justin at querki dot net)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
