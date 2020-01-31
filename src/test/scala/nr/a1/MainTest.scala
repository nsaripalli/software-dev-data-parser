package nr.a1

import org.scalatest.funspec.AnyFunSpec

class MainTest extends AnyFunSpec {
    describe("line parsing") {
    describe("ints") {
      it("should parse simple ints") {
        assert(ParseLine("<+1><-2>") == Seq(1, -2))
        assert(ParseLine("<2><3>") == Seq(2, 3))
        assert(ParseLine("<27>") == Seq(27))
      }
      it("should parse simple ints with spaces") {
        assert(ParseLine("<2>  <3> <4>   <-1>") == Seq(2, 3, 4, -1))
        assert(ParseLine(" <27> ") == Seq(27))
      }
      it("should parse simple ints with spaces inside the <>") {
        assert(ParseLine("< 2 > < 3> <4 >") == Seq(2, 3, 4))
        assert(ParseLine("<    27     >") == Seq(27))
      }
    }

    describe("bools") {
      it("should parse simple bools") {
        assert(ParseLine("<1><0>") == Seq(true, false))
        assert(ParseLine("<0><1>") == Seq(false, true))
        assert(ParseLine("<0>") == Seq(false))
      }
      it("should parse simple bools with spaces") {
        assert(ParseLine("<0>  <0> <0>   <1>") == Seq(false, false, false, true))
        assert(ParseLine(" <1> ") == Seq(true))
      }
      it("should parse simple bools with spaces inside the <>") {
        assert(ParseLine("< 1 > < 1> <0 >") == Seq(true, true, false))
        assert(ParseLine("<    0     >") == Seq(false))
      }
    }

    describe("floats") {
      it("should parse simple floats") {
        assert(ParseLine("<1.0><2.0>") == Seq(1.0f, 2.0f))
        assert(ParseLine("<2.005005><3.99>") == Seq(2.005005f, 3.99f))
        assert(ParseLine("<0.275>") == Seq(0.275f))
      }
      it("should parse simple floats with spaces") {
        assert(ParseLine("<0.2>  <3.3> <4.5>   <1.0>") == Seq(0.2f, 3.3f, 4.5f, 1.0f))
        assert(ParseLine(" <27.10101> ") == Seq(27.10101f))
      }
      it("should parse simple floats with spaces inside the <>") {
        assert(ParseLine("< 1.0 > < 3.2> <0.4 >") == Seq(1.0f, 3.2f, 0.4f))
        assert(ParseLine("<    27.002     >") == Seq(27.002f))
      }
    }

    describe("strings") {
      it("should parse semi-complex strings") {
        assert(ParseLine("""<"hello"><"hello"><"hello">""") == Seq("hello", "hello", "hello"))
        assert(ParseLine("""<"hello    " >< "  hello  "><  "hello"   >""") == Seq("hello    ", "  hello  ", "hello"))
        assert(ParseLine("""<"asd"> <"">      <hello>""") == Seq("asd", "", "hello"))
        assert(ParseLine("""<"yolo"><"hello"><"2">""") == Seq("yolo", "hello", "2"))
      }
      it("should deal with nested < fine") {
        assert(ParseLine(""" <<<<<<<<<<<<<> """)
          == Seq("<<<<<<<<<<<<"))
      }
      it("should deal with nested <> fine") {
        assert(ParseLine("""<"<<<<<<<<  <><><<<>>>><>  >><><><><>>>>>>>"> <"<          >"> <"<>">""")
          == Seq("<<<<<<<<  <><><<<>>>><>  >><><><><>>>>>>>", "<          >", "<>"))
      }
      it("should parse complex tests") {
        assert(ParseLine("""<"hi"> < hi > <"<"> <<>""") == Seq("hi", "hi", "<", "<"))
      }
      it("should parse the empty strings") {
        assert(ParseLine("""<""> <"">""") == Seq("", ""))
      }
    }
  }
}
