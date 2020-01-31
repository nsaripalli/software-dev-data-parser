package nr.a1

import scala.io.Source
import scala.util._
import java.io._
import java.nio.file.{Files, Paths, StandardCopyOption}

/**
  * Config:
  * Represents a container for the command line arguments
  * and their sentinel values.
  *
  * immutable
  *
  * Authors: dermer.s@husky.neu.edu && saripalli.n@husky.neu.edu
  */
case class Config(f: String = "",
                  from: Int = -1,
                  len: Long = -1l,
                  print_col_type: Int = -1,
                  print_col_idx: (Int, Int) = (-1, -1),
                  is_missing_idx: (Int, Int) = (-1, -1))

/**
  * Type:
  * Represents an enum of possible schema types in a SOR file
  * as well as an internal representation for a missing value.
  * Each type is aware of its "value" in the hierarchy and
  * its name as should be printed to the user.
  *
  * immutable
  *
  * Authors: dermer.s@husky.neu.edu && saripalli.n@husky.neu.edu
  */
sealed trait Type {
  def value: Int

  def name: String
} // Enum of types that can be in a schema
case object pStr extends Type {
  val value = 4
  val name = "STRING"
} // String
case object pFlt extends Type {
  val value = 3
  val name = "FLOAT"
} // Float
case object pInt extends Type {
  val value = 2
  val name = "INTEGER"
} // Integer
case object pBol extends Type {
  val value = 1
  val name = "BOOL"
} // Boolean
case object pEmt extends Type {
  val value = 0
  val name = "MISSING"
} // Missing

object Main extends App {
  @scala.annotation.tailrec
  def nextOption(config: Config, list: List[String]): Config = {
    list match {
      case Nil => config
      case "-f" :: value :: tail =>
        nextOption(config.copy(f = value), tail)
      case "-from" :: value :: tail =>
        nextOption(config.copy(from = value.toInt), tail)
      case "-len" :: value :: tail =>
        nextOption(config.copy(len = value.toLong), tail)
      case "-print_col_type" :: value :: tail =>
        nextOption(config.copy(print_col_type = value.toInt), tail)
      case "-print_col_idx" :: value1 :: value2 :: tail =>
        nextOption(config.copy(print_col_idx = (value1.toInt, value2.toInt)), tail)
      case "-is_missing_idx" :: value1 :: value2 :: tail =>
        nextOption(config.copy(is_missing_idx = (value1.toInt, value2.toInt)), tail)
      case _ => throw new IllegalArgumentException("Invalid command line arguments")
    }
  }

  val conf = nextOption(Config(), args.toList)

  // validation
  if (conf.from < 0 || conf.len < 0)
    throw new IllegalArgumentException("From and Len must be whole integers")
  (conf.print_col_type, conf.print_col_idx, conf.is_missing_idx) match {
    case(-1, (-1, -1), (-1, -1)) => throw new IllegalArgumentException("Must make at least one query")
    case _ => // do nothing because validation passed
  }

  // number setup
  val from = conf.from
  val len = conf.len
  val dropNum = if (len == 0) 0 else 1

  // file setup
  val filepath = Files.copy(
    Paths.get(conf.f),
    Paths.get(s"${conf.f}.cp"),
    StandardCopyOption.REPLACE_EXISTING
  )
  val filename = filepath.getFileName.toString

  // check if we need to truncate the last line
  val truncate: Int = if ((from + len) < filepath.toFile.length) 1: Int else 0: Int // Manual type hinting because intellij

  // determine schema size
  val numCols = Using {
    val raf = new RandomAccessFile(filename, "rw")
    raf.skipBytes(from)
    if (truncate == 1) raf.setLength(from + len)
    Source.fromInputStream(new BufferedInputStream(new FileInputStream(raf.getFD)))
  } { file =>
    file.getLines().slice(from, 500 + from - truncate).foldLeft(0) { (acc, line) =>
      val parsedLine = ParseLine(line)
      if (parsedLine.size > acc) parsedLine.size
      else acc
    }
  } match {
    case Success(value) => value
    case Failure(exception) => throw exception
  }

  // determine schema
  val schema = Using {
    val raf = new RandomAccessFile(filename, "rw")
    raf.skipBytes(from)
    Source.fromInputStream(new BufferedInputStream(new FileInputStream(raf.getFD)))
  } { file =>
    file.getLines().slice(from, 500 + from - truncate).foldLeft(Seq.fill[Type](numCols)(pEmt)) { (acc, line) =>
      val ogParsedLine = ParseLine(line)
      val parsedLine = ogParsedLine ++ Seq.fill[Type](numCols - ogParsedLine.size)(pEmt)
      val zipped = parsedLine.zip(acc)

      zipped.map { e =>
        val lineElem = e._1
        val accElem = e._2
        lineElem match {
          case _: String => pStr
          case _: Float if accElem.value < pStr.value => pFlt
          case _: Int if accElem.value < pFlt.value => pInt
          case _: Boolean if accElem.value < pInt.value => pBol
          case _ => accElem
        }
      }
    }
  } match {
    case Success(value) => value
    case Failure(exception) => throw exception
  }

  // respond to queries
  if (conf.print_col_type >= 0) {
    if (conf.print_col_type >= schema.size)
      throw new IllegalArgumentException(s"The column inputted is greater than ${schema.size}, the size of the schema")
    println(schema(conf.print_col_type).name)
  }
  if (conf.print_col_idx._1 >= 0 && conf.print_col_idx._2 >= 0) {
    parseVal(conf.print_col_idx._1, conf.print_col_idx._2)
  }
  if (conf.is_missing_idx._1 >= 0 && conf.is_missing_idx._2 >= 0) {
    parseVal(conf.is_missing_idx._1, conf.is_missing_idx._2, checkMissing = true)
  }

  /**
    * Used in conjunction with the implicit below to add a string to int method
    * to the String class based on how our parsed strings should look
    *
    * Authors: dermer.s@husky.neu.edu && saripalli.n@husky.neu.edu
    */
  class asInt(s: String) {
    def asInt: Int = s match {
      case "true" => 1
      case "false" => 0
      case _ => s.toInt
    }
  }

  implicit def convertStringToInt(s: String): asInt = new asInt(s)

  /**
    * Used in conjunction with the implicit below to add a string to float method
    * to the String class based on how our parsed strings should look
    *
    * Authors: dermer.s@husky.neu.edu && saripalli.n@husky.neu.edu
    */
  class asFloat(s: String) {
    def asFloat: Float = s match {
      case "true" => 1f
      case "false" => 0f
      case _ => s.toFloat
    }
  }

  implicit def convertStringToFloat(s: String): asFloat = new asFloat(s)

  /**
   * Will take the input file and parse the value inside a <> @ col c offset r.
   * Returns whether or not the value is missing if checkMissing is true instead
   * of the parsed value itself.
   */
  def parseVal(c: Int, r: Int, checkMissing: Boolean = false): Unit = {
    Using(Source.fromFile(conf.f)) { file =>
      val lines = file.getLines()
      if (c > schema.size || r > lines.length) {
        println(s"The coordinate ($c, $r) is out of bounds of the SOR")
        return
      }
      val sliced = lines.slice(r, r + 1).toSeq
      sliced.foreach { line =>
        val ogParsedLine = ParseLine(line)
        val parsedLine = ParseLine(line) ++ Seq.fill[Any](schema.size - ogParsedLine.size)(pEmt)
        if (parsedLine.size > schema.size) throw new IllegalArgumentException(s"($r, $c) violates the schema")
        if (parsedLine(c) == pEmt) {
          println(pEmt.name)
        } else {
          schema(c) match {
            case `pStr` => if (checkMissing) println(0) else println(s""""${parsedLine(c)}"""")
            case `pFlt` => if (checkMissing) println(0) else println(parsedLine(c).toString.asFloat)
            case `pInt` => if (checkMissing) println(0) else println(parsedLine(c).toString.asInt)
            case `pBol` => if (checkMissing) println(0) else println(if (parsedLine(c).toString.toBoolean) 1 else 0)
            case `pEmt` => if (checkMissing) println(1) else println("That value is empty")
          }
        }
      }
    } match {
      case Success(_) => // Do nothing
      case Failure(exception) => {
        println(s"The value at $c, $r violates the schema. This caused the following internal error:")
        throw exception
      }
    }
  }

  Files.delete(filepath)
}


/**
  * ParseLine: Singleton for the parsing functions (per line)
  * Parses one line assuming types
  *
  * Design Decisions:
  * - < and > have higher precedence than "
  * I.E. """<><>""" is (missing val, missing val)
  * and """<"><">""" is ("><")
  * BUT """<<>""" is unambiguous and thus ok
  * - " are the only valid quotes (not ')
  *
  * note: immutable
  *
  * Authors: dermer.s@husky.neu.edu && saripalli.n@husky.neu.edu
  */
object ParseLine {
  def apply(line: String): Seq[Any] = {
    regexMatch(line)
  }

  def regexMatch(input: String): Seq[Any] = {
    // Credit to https://stackoverflow.com/a/22184202
    // for the regex on 1/22/2020
    val element = raw"""<[^'">]*(("[^"]*"|'[^']*')[^'">]*)*>""".r
    val mi = element.findAllIn(input)

    mi.foldLeft(Seq[Any]()) { (acc, e) =>
      acc :+ regexMatchTypes(e)
    }
  }

  def regexMatchTypes(substring: String): Any = {
    val cleanedSub = substring.trim.stripPrefix("<").stripSuffix(">").trim
    val tryToInt = Try(cleanedSub.toInt)
    val tryToFloat = Try(cleanedSub.toFloat)
    (cleanedSub, tryToInt, tryToFloat) match {
      case ("0", _, _) => false
      case ("1", _, _) => true
      case (_, Success(value), _) => value
      case (_, _, Success(value)) => value
      case ("", _, _) => pEmt
      case _ => cleanedSub.stripPrefix(""""""").stripSuffix(""""""") // """"""" == "\""
    }
  }
}
