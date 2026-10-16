package io.github.nayasis.kotlin.basica.core.string

import io.kotest.core.spec.style.StringSpec
import io.kotest.assertions.withClue
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

    "change Hangul Josa : all scenarios" {
        // template written in any form -> (after jongsung, after no jongsung)
        val scenarios = listOf(
            listOf("{}은 맛있다",   "{}는 맛있다",   "수박은 맛있다",   "사과는 맛있다"),
            listOf("{}이 있다",     "{}가 있다",     "수박이 있다",     "사과가 있다"),
            listOf("{}을 먹다",     "{}를 먹다",     "수박을 먹다",     "사과를 먹다"),
            listOf("{}과 귤",       "{}와 귤",       "수박과 귤",       "사과와 귤"),
            listOf("{}으로 만든",   "{}로 만든",     "수박으로 만든",   "사과로 만든"),
            listOf("{}으로서",      "{}로서",        "수박으로서",      "사과로서"),
            listOf("{}으로써",      "{}로써",        "수박으로써",      "사과로써"),
            listOf("{}으로부터",    "{}로부터",      "수박으로부터",    "사과로부터"),
            listOf("{}이라는 과일", "{}라는 과일",   "수박이라는 과일", "사과라는 과일"),
            listOf("{}이다.",       "{}다.",         "수박이다.",       "사과다."),
            listOf("{}이라고",      "{}라고",        "수박이라고",      "사과라고"),
            listOf("{}이라도",      "{}라도",        "수박이라도",      "사과라도"),
            listOf("{}이라면",      "{}라면",        "수박이라면",      "사과라면"),
            listOf("{}이라서",      "{}라서",        "수박이라서",      "사과라서"),
            listOf("{}이란 ",       "{}란 ",         "수박이란 ",       "사과란 "),
            listOf("{}이나 ",       "{}나 ",         "수박이나 ",       "사과나 "),
            listOf("{}이나마",      "{}나마",        "수박이나마",      "사과나마"),
            listOf("{}이든지",      "{}든지",        "수박이든지",      "사과든지"),
            listOf("{}이든가",      "{}든가",        "수박이든가",      "사과든가"),
            listOf("{}이든 ",       "{}든 ",         "수박이든 ",       "사과든 "),
            listOf("{}이랑",        "{}랑",          "수박이랑",        "사과랑"),
            listOf("{}이야말로",    "{}야말로",      "수박이야말로",    "사과야말로"),
            listOf("{}이여,",       "{}여,",         "수박이여,",       "사과여,"),
            listOf("{}이시여",      "{}시여",        "수박이시여",      "사과시여"),
            listOf("{}이에요",      "{}예요",        "수박이에요",      "사과예요"),
            listOf("{}이에요",      "{}에요",        "수박이에요",      "사과예요"),
            listOf("{}이었다",      "{}였다",        "수박이었다",      "사과였다"),
            listOf("{}이었다",      "{}었다",        "수박이었다",      "사과였다"),
            listOf("{}이어서",      "{}여서",        "수박이어서",      "사과여서"),
            listOf("{}이어도",      "{}여도",        "수박이어도",      "사과여도"),
            listOf("{}이어야",      "{}여야",        "수박이어야",      "사과여야"),
            listOf("{}이지만",      "{}지만",        "수박이지만",      "사과지만"),
            listOf("{}이니까",      "{}니까",        "수박이니까",      "사과니까"),
            listOf("{}이므로",      "{}므로",        "수박이므로",      "사과므로"),
            listOf("{}이며,",       "{}며,",         "수박이며,",       "사과며,"),
            listOf("{}이고,",       "{}고,",         "수박이고,",       "사과고,"),
            listOf("{}이면 ",       "{}면 ",         "수박이면 ",       "사과면 "),
            listOf("{}이지?",       "{}지?",         "수박이지?",       "사과지?"),
            listOf("{}이니?",       "{}니?",         "수박이니?",       "사과니?"),
            listOf("{}이냐?",       "{}냐?",         "수박이냐?",       "사과냐?"),
            listOf("{}이요,",       "{}요,",         "수박이요,",       "사과요,"),
            listOf("{}이세요",      "{}세요",        "수박이세요",      "사과세요"),
            listOf("{}이셨다",      "{}셨다",        "수박이셨다",      "사과셨다"),
            listOf("{}이죠",        "{}이죠",        "수박이죠",        "사과죠"),
            listOf("{}이네",        "{}이네",        "수박이네",        "사과네"),
            listOf("{}이던",        "{}이던",        "수박이던",        "사과던"),
            listOf("{}아,",         "{}야,",         "수박아,",         "사과야,"),
            // written with parentheses
            listOf("{}은(는)",      "{}는(은)",      "수박은",          "사과는"),
            listOf("{}이(가)",      "{}가(이)",      "수박이",          "사과가"),
            listOf("{}을(를)",      "{}를(을)",      "수박을",          "사과를"),
            listOf("{}과(와)",      "{}와(과)",      "수박과",          "사과와"),
            listOf("{}아(야)",      "{}야(아)",      "수박아",          "사과야"),
            listOf("{}(으)로",      "{}(으)로",      "수박으로",        "사과로"),
            listOf("{}(이)라는",    "{}(이)라는",    "수박이라는",      "사과라는"),
            listOf("{}(이)나",      "{}(이)나",      "수박이나",        "사과나"),
            listOf("{}(이)에요",    "{}(이)에요",    "수박이에요",      "사과예요"),
            listOf("{}(이)었다",    "{}(이)었다",    "수박이었다",      "사과였다"),
        )
        for( (withJongsung, withoutJongsung, expectedWith, expectedWithout) in scenarios ) {
            for( template in listOf(withJongsung, withoutJongsung) ) {
                withClue(template) {
                    binder.bind(template, "수박") shouldBe expectedWith
                    binder.bind(template, "사과") shouldBe expectedWithout
                }
            }
        }
    }

    "change Hangul Josa : rieul with 으로" {
        binder.bind("{}으로", "서울") shouldBe "서울로"
        binder.bind("{}(으)로", "서울") shouldBe "서울로"
        binder.bind("{}으로부터", "서울") shouldBe "서울로부터"
        binder.bind("{}로", "닭") shouldBe "닭으로"
    }

    "keep copula '이' which can not be omitted" {
        binder.bind("{}이자 배우", "가수") shouldBe "가수이자 배우"
        binder.bind("{}이자 배우", "감독") shouldBe "감독이자 배우"
        binder.bind("{}이상", "10개") shouldBe "10개이상"
        binder.bind("{}이상", "사과") shouldBe "사과이상"
        binder.bind("{}이하", "사과") shouldBe "사과이하"
        binder.bind("{}이내", "사과") shouldBe "사과이내"
        binder.bind("{}인 경우", "사과") shouldBe "사과인 경우"
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
    }

    "change Hangul Josa : pronunciation markup of non-english" {
        binder.bind("{}을 방문했다.", "{{東京|도쿄}}") shouldBe "東京를 방문했다."
        binder.bind("{}을 방문했다.", "{{北京|베이징}}") shouldBe "北京을 방문했다."
        binder.bind("{}은 맛있다.", "{{ラーメン|라멘}}") shouldBe "ラーメン은 맛있다."
        binder.bind("{}이 살았다.", "{{Renée|르네}}") shouldBe "Renée가 살았다."
        binder.bind("{}으로 간다.", "{{München|뮌헨}}") shouldBe "München으로 간다."
        binder.bind("{}으로 간다.", "{{Москва|모스크바}}") shouldBe "Москва로 간다."
        binder.bind("{}과 함께", "{{Ελλάδα|그리스}}") shouldBe "Ελλάδα와 함께"
        binder.bind("{}이 살았다.", "{{محمد|무함마드}}") shouldBe "محمد가 살았다."
    }

    "print pronunciation markup as it is" {
        binder.bind("{name}이 살았다.", mapOf("name" to "\\{{James|제임스}}")) shouldBe "{{James|제임스}}가 살았다."
        binder.bind("{name}이 살았다.", mapOf("name" to "\\{{James|제임스}}, {{Tom|톰}}")) shouldBe "{{James|제임스}}, Tom이 살았다."
        binder.bind("{name}이 살았다.", mapOf("name" to "{{John|존}}"), modifyKorean = false) shouldBe "{{John|존}}이 살았다."
    }

    "keep braces in binding value" {
        binder.bind("{}", "{{abc}}") shouldBe "{{abc}}"
        binder.bind("{{{}}}", "{abc}") shouldBe "{{abc}}"
        binder.bind("{} {{merong}}", "a}}b") shouldBe "a}}b {merong}"
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
        binder.bind("{}가능", "수박") shouldBe "수박가능"
        binder.bind("{}가격", "수박") shouldBe "수박가격"
        binder.bind("{}란에 입력", "이름") shouldBe "이름란에 입력"
        binder.bind("{}나라", "수박") shouldBe "수박나라"
        binder.bind("{}다음", "수박") shouldBe "수박다음"
        binder.bind("{}고객", "수박") shouldBe "수박고객"
        binder.bind("{}지역", "수박") shouldBe "수박지역"
        binder.bind("{}면적", "수박") shouldBe "수박면적"
        binder.bind("{}요청", "수박") shouldBe "수박요청"
        binder.bind("{}라인", "수박") shouldBe "수박라인"
        binder.bind("{}과정", "교육") shouldBe "교육과정"
        binder.bind("{}과목", "사회") shouldBe "사회과목"
        binder.bind("{}와인", "수박") shouldBe "수박와인"
        binder.bind("{}은행", "사과") shouldBe "사과은행"
        binder.bind("{}는지", "수박") shouldBe "수박는지"
        binder.bind("{}을지로", "사과") shouldBe "사과을지로"
        binder.bind("{}로그인", "수박") shouldBe "수박로그인"
        binder.bind("{}로딩", "수박") shouldBe "수박로딩"
    }

    "change Hangul Josa followed by another josa" {
        binder.bind("{}과의 대화", "사과") shouldBe "사과와의 대화"
        binder.bind("{}와도 같다", "수박") shouldBe "수박과도 같다"
        binder.bind("{}과는 다르다", "사과") shouldBe "사과와는 다르다"
        binder.bind("{}과만 친하다", "사과") shouldBe "사과와만 친하다"
        binder.bind("{}은요?", "사과") shouldBe "사과는요?"
        binder.bind("{}를요?", "수박") shouldBe "수박을요?"
        binder.bind("{}로는 부족하다", "수박") shouldBe "수박으로는 부족하다"
        binder.bind("{}로도 충분하다", "수박") shouldBe "수박으로도 충분하다"
        binder.bind("{}로만 만든다", "수박") shouldBe "수박으로만 만든다"
        binder.bind("{}로의 여행", "수박") shouldBe "수박으로의 여행"
        binder.bind("{}로요", "수박") shouldBe "수박으로요"
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