package io.github.nayasis.kotlin.basica.core.string

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.File

internal class FormatterTest: StringSpec({

    val binder = Formatter()

    "change Hangul Josa" {
        binder.bind("{}를 등록합니다.", "카드템플릿") shouldBe "카드템플릿을 등록합니다."
        binder.bind("{}를 등록합니다.", "카드") shouldBe "카드를 등록합니다."
        binder.bind("{}는 등록됩니다.", "카드") shouldBe "카드는 등록됩니다."
        binder.bind("{}는 등록됩니다.", "카드템플릿") shouldBe "카드템플릿은 등록됩니다."
        binder.bind("{}가 등록됩니다.", "카드") shouldBe "카드가 등록됩니다."
        binder.bind("{}가 등록됩니다.", "카드템플릿") shouldBe "카드템플릿이 등록됩니다."
    }

    "change Hangul Josa : 과/와, 으로/로" {
        binder.bind("{}과 함께", "사과") shouldBe "사과와 함께"
        binder.bind("{}와 함께", "수박") shouldBe "수박과 함께"
        binder.bind("{}로 이동", "집") shouldBe "집으로 이동"
        binder.bind("{}으로 이동", "학교") shouldBe "학교로 이동"
        binder.bind("{}으로 이동", "서울") shouldBe "서울로 이동"
        binder.bind("{}로서 역할", "관리자") shouldBe "관리자로서 역할"
        binder.bind("{}로써 증명", "결과") shouldBe "결과로써 증명"
    }

    "change Hangul Josa : others" {
        binder.bind("{}나 귤", "사과") shouldBe "사과나 귤"
        binder.bind("{}나 귤", "수박") shouldBe "수박이나 귤"
        binder.bind("{}랑 놀자", "민준") shouldBe "민준이랑 놀자"
        binder.bind("{}야, 밥 먹자", "민준") shouldBe "민준아, 밥 먹자"
        binder.bind("{}아, 밥 먹자", "민수") shouldBe "민수야, 밥 먹자"
        binder.bind("{}여, 일어나라", "겨레") shouldBe "겨레여, 일어나라"
        binder.bind("{}여, 일어나라", "민족") shouldBe "민족이여, 일어나라"
        binder.bind("{}라고 합니다", "홍길동") shouldBe "홍길동이라고 합니다"
        binder.bind("{}이라는 이름", "철수") shouldBe "철수라는 이름"
        binder.bind("{}라도 좋다", "물") shouldBe "물이라도 좋다"
        binder.bind("{}예요", "책") shouldBe "책이에요"
        binder.bind("{}이에요", "사과") shouldBe "사과예요"
        binder.bind("{}였다", "학생") shouldBe "학생이었다"
        binder.bind("{}이었다", "의사") shouldBe "의사였다"
    }

    "change Hangul Josa : copula" {
        binder.bind("{}이다", "사과") shouldBe "사과다"
        binder.bind("{}이다", "수박") shouldBe "수박이다"
        binder.bind("{}이며", "사과") shouldBe "사과며"
        binder.bind("{}이고", "사과") shouldBe "사과고"
        binder.bind("{}이 있다", "사과") shouldBe "사과가 있다"
        binder.bind("{}이", "사과") shouldBe "사과가"
    }

    "change Hangul Josa : digit" {
        binder.bind("{}는 짝수", 2) shouldBe "2는 짝수"
        binder.bind("{}는 홀수", 3) shouldBe "3은 홀수"
        binder.bind("{}로 나눈다", 1) shouldBe "1로 나눈다"
        binder.bind("{}로 나눈다", 3) shouldBe "3으로 나눈다"
        binder.bind("{}을 더한다", 10) shouldBe "10을 더한다"
        binder.bind("{}를 입력하세요", "abc") shouldBe "abc를 입력하세요"
    }

    "change Hangul Josa : skip quotes and brackets" {
        binder.bind("'{}'는 필수입니다", "이름") shouldBe "'이름'은 필수입니다"
        binder.bind("「{}」가 없습니다", "파일") shouldBe "「파일」이 없습니다"
        binder.bind("{}가 접속했습니다", "홍길동(관리자)") shouldBe "홍길동(관리자)가 접속했습니다"
        binder.bind("{}가 접속했습니다", "관리자(홍길동)") shouldBe "관리자(홍길동)이 접속했습니다"
        binder.bind("'{}' 확인", "이름") shouldBe "'이름' 확인"
    }

    "change Hangul Josa : pronunciation markup" {
        binder.bind("{name}이 살았다.", mapOf("name" to "{{James|제임스}}")) shouldBe "James가 살았다."
        binder.bind("{name}이 살았다.", mapOf("name" to "{{Tom|톰}}")) shouldBe "Tom이 살았다."
        binder.bind("{name}이 살았다.", mapOf("name" to "{{제임스|제임스}}")) shouldBe "제임스가 살았다."
        binder.bind("{name}이 살았다.", mapOf("name" to "Mr. {{Smith|스미스}}")) shouldBe "Mr. Smith가 살았다."
        binder.bind("{name}이 살았다.", mapOf("name" to "{{Nick|닉}} 2세")) shouldBe "Nick 2세가 살았다."
        binder.bind("{name}이 살았다.", mapOf("name" to "{{John|존}}"), modifyKorean = false) shouldBe "John이 살았다."
    }

    "change Hangul Josa : english" {
        binder.bind("{name}이 살았다.", mapOf("name" to "James")) shouldBe "James가 살았다."
        binder.bind("{name}이 살았다.", mapOf("name" to "{James}")) shouldBe "{James}가 살았다."
        binder.bind("{}가 살았다.", "Tom") shouldBe "Tom이 살았다."
        binder.bind("{}가 살았다.", "John") shouldBe "John이 살았다."
        binder.bind("{}는 과일", "Apple") shouldBe "Apple은 과일"
        binder.bind("{}으로 검색", "Google") shouldBe "Google로 검색"
        binder.bind("{}을 실행", "Chrome") shouldBe "Chrome을 실행"
        binder.bind("{}을 설치", "app") shouldBe "app을 설치"
        binder.bind("{}을 사용", "Kotlin") shouldBe "Kotlin을 사용"
        binder.bind("{}를 열기", "Spring") shouldBe "Spring을 열기"
        binder.bind("{}을 입력", "Mike") shouldBe "Mike를 입력"
        binder.bind("{}을 입력", "Peter") shouldBe "Peter를 입력"
        binder.bind("{}가 살았다.", "Nick") shouldBe "Nick이 살았다."
        binder.bind("{}가 살았다.", "Bob") shouldBe "Bob이 살았다."
        binder.bind("{}는 동물", "cat") shouldBe "cat은 동물"
        binder.bind("{}는 회사", "IBM") shouldBe "IBM은 회사"
        binder.bind("{}로 조회", "SQL") shouldBe "SQL로 조회"
        binder.bind("{}는 약어", "API") shouldBe "API는 약어"
    }

    "keep words starting with josa-like character" {
        binder.bind("{}야간 근무", "민준") shouldBe "민준야간 근무"
        binder.bind("{}여부", "민족") shouldBe "민족여부"
    }

    "skip" {
        binder.bind("{{}}") shouldBe "{}"
        binder.bind("1{}2") shouldBe "12"
    }

    "complex" {
        val format = "{aa} {} {{merong}} is {a} and {{{b}"
        val param = mapOf<String,Any>( "aa" to "nayasis", "b" to "end")
        binder.bind(format, param, 2) shouldBe "nayasis 2 {merong} is  and {end"
    }

    "no parameter" {
        val format = "badCredentials"
        binder.bind(format) shouldBe format
    }

    "format" {
        val parameter = "{'name':'abc', 'age':2}".toMap()
        binder.bind("PRE {{age}} POST", parameter) shouldBe "PRE {age} POST"
        binder.bind("{}{}", "001", "001") shouldBe "001001"
        binder.bind("items : (count:{})\n{}", 3) shouldBe "items : (count:3)\n"
        binder.bind("{ name : {}, age : {} }", "merong", 2) shouldBe "{ name : merong, age : 2 }"
        binder.bind("{}\n{}\n{}", "5K", "Util", "desc") shouldBe "5K\nUtil\ndesc"
        binder.bind("PRE {age} POST", parameter) shouldBe "PRE 2 POST"
        binder.bind("{name} PRE {age} POST", parameter) shouldBe "abc PRE 2 POST"
        binder.bind("{name} PRE {age:%3d} POST", parameter) shouldBe "abc PRE   2 POST"
    }

    "format from Bean" {
        val bean = Pojo(1,2,"abcd")
        binder.bind("{a} is {b} or {c}", bean) shouldBe "1 is 2 or abcd"
    }

    "format from map" {
        val bean = "{'a':1, 'b':2, 'c':'abcd'}".toMap()
        binder.bind("{a} is {b} or {c}", bean) shouldBe "1 is 2 or abcd"
    }

    "file parameter" {
        val f = File("salamander")
        binder.bind("it is {:%15s}", f) shouldBe "it is      salamander"
    }

})

data class Pojo (
    val a: Int = 0,
    val b: Int = 0,
    val c: String? = null,
)