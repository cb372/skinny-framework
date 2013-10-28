package skinny.assets

import org.scalatest._
import org.scalatest.matchers._

class CoffeeScriptCompilerSpec extends FlatSpec with ShouldMatchers {

  behavior of "CoffeeScriptCompiler"

  it should "compile code" in {
    val compiler = CoffeeScriptCompiler()
    val js = compiler.compile(
      """class Person
        |  constructor: (name, email) ->
        |    @name = name
        |    @email = email
        |  name: 'Anonymous'
        |  email: null
        |  sayHello: -> console.log "My name is #{@name}!"
        |
        |bob = new Person('Bob')
        |bob.sayHello()
      """.stripMargin)

    js should equal(
      """(function() {
        |  var Person, bob;
        |
        |  Person = (function() {
        |    function Person(name, email) {
        |      this.name = name;
        |      this.email = email;
        |    }
        |
        |    Person.prototype.name = 'Anonymous';
        |
        |    Person.prototype.email = null;
        |
        |    Person.prototype.sayHello = function() {
        |      return console.log("My name is " + this.name + "!");
        |    };
        |
        |    return Person;
        |
        |  })();
        |
        |  bob = new Person('Bob');
        |
        |  bob.sayHello();
        |
        |}).call(this);
        |""".stripMargin)
  }

}