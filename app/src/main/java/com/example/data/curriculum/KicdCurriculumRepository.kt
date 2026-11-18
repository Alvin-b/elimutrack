package com.example.data.curriculum

import com.example.domain.model.ApprovedResource
import com.example.domain.model.KicdDesignItem
import com.example.domain.model.PathwayInfo
import com.example.domain.model.TimetableSlot

/**
 * Official KICD Competency-Based Education (CBE/CBC) curriculum guidelines repository
 * covering Lower Primary (Grades 1-3), Upper Primary (Grades 4-6), and Junior School (Grades 7-9).
 */
object KicdCurriculumRepository {

  val cbcGrades: List<String> = listOf(
    "Grade 1", "Grade 2", "Grade 3",
    "Grade 4", "Grade 5", "Grade 6",
    "Grade 7", "Grade 8", "Grade 9",
  )

  /** Grades with bundled curriculum templates. Timetables remain Grade 1-9. */
  val curriculumGrades: List<String> = listOf("Pre-Primary 1", "Pre-Primary 2") + cbcGrades

  val cbcCoreCompetencies: List<String> = listOf(
    "Communication and Collaboration",
    "Critical Thinking and Problem Solving",
    "Creativity and Imagination",
    "Citizenship",
    "Digital Literacy",
    "Learning to Learn",
    "Self-Efficacy",
  )

  val cbcValues: List<String> = listOf(
    "Love", "Responsibility", "Respect", "Unity", "Peace", "Patriotism", "Social Justice", "Integrity"
  )

  val cbcLearningAreasByLevel: Map<String, List<String>> = mapOf(
    "Lower Primary" to listOf(
      "Mathematical Activities",
      "English Language",
      "Kiswahili Language",
      "Literacy Activities",
      "Environmental Activities",
      "Creative Activities",
      "Religious Activities",
    ),
    "Upper Primary" to listOf(
      "Mathematics",
      "English",
      "Kiswahili",
      "Science & Technology",
      "Social Studies",
      "Agriculture & Nutrition",
      "Creative Arts",
      "Religious Education",
    ),
    "Junior School" to listOf(
      "Mathematics",
      "English",
      "Kiswahili",
      "Integrated Science",
      "Social Studies",
      "Agriculture & Nutrition",
      "Pre-Technical Studies",
      "Creative Arts & Sports",
      "Religious Education",
    ),
  )

  private val prePrimarySubjects: List<com.example.domain.model.SubjectInfo> = listOf(
    com.example.domain.model.SubjectInfo("math_p", "Mathematics"),
    com.example.domain.model.SubjectInfo("eng_p", "English Language"),
    com.example.domain.model.SubjectInfo("kis_p", "Kiswahili"),
    com.example.domain.model.SubjectInfo("env_p", "Environmental Activities"),
    com.example.domain.model.SubjectInfo("crt_p", "Creative Arts"),
    com.example.domain.model.SubjectInfo("rel_p", "Religious Education"),
  )

  private val lowerPrimarySubjects: List<com.example.domain.model.SubjectInfo> = listOf(
    com.example.domain.model.SubjectInfo("math_p", "Mathematics"),
    com.example.domain.model.SubjectInfo("eng_p", "English Language"),
    com.example.domain.model.SubjectInfo("kis_p", "Kiswahili"),
    com.example.domain.model.SubjectInfo("indig_p", "Indigenous Languages"),
    com.example.domain.model.SubjectInfo("env_p", "Environmental Activities"),
    com.example.domain.model.SubjectInfo("crt_p", "Creative Arts"),
    com.example.domain.model.SubjectInfo("rel_p", "Religious Education"),
  )

  private val upperPrimarySubjects: List<com.example.domain.model.SubjectInfo> = listOf(
    com.example.domain.model.SubjectInfo("math_p", "Mathematics"),
    com.example.domain.model.SubjectInfo("eng_p", "English Language"),
    com.example.domain.model.SubjectInfo("kis_p", "Kiswahili"),
    com.example.domain.model.SubjectInfo("indig_p", "Indigenous Languages"),
    com.example.domain.model.SubjectInfo("env_p", "Environmental Activities"),
    com.example.domain.model.SubjectInfo("sost", "Social Studies"),
    com.example.domain.model.SubjectInfo("agri", "Agriculture & Nutrition"),
    com.example.domain.model.SubjectInfo("crt_p", "Creative Arts"),
    com.example.domain.model.SubjectInfo("rel_p", "Religious Education"),
  )

  private val juniorSubjects: List<com.example.domain.model.SubjectInfo> = listOf(
    com.example.domain.model.SubjectInfo("eng", "English"),
    com.example.domain.model.SubjectInfo("kis", "Kiswahili"),
    com.example.domain.model.SubjectInfo("math", "Mathematics"),
    com.example.domain.model.SubjectInfo("isci", "Integrated Science"),
    com.example.domain.model.SubjectInfo("sost", "Social Studies"),
    com.example.domain.model.SubjectInfo("agri", "Agriculture & Nutrition"),
    com.example.domain.model.SubjectInfo("ptech", "Pre-Technical Studies"),
    com.example.domain.model.SubjectInfo("arts", "Creative Arts & Sports"),
    com.example.domain.model.SubjectInfo("reled", "Religious Education"),
  )

  private val languageSubjects: List<com.example.domain.model.SubjectInfo> = listOf(
    com.example.domain.model.SubjectInfo("arabic", "Arabic"),
    com.example.domain.model.SubjectInfo("chinese", "Mandarin Chinese"),
    com.example.domain.model.SubjectInfo("french", "French"),
  )

  val primarySubjects: List<com.example.domain.model.SubjectInfo> =
    (prePrimarySubjects + lowerPrimarySubjects + upperPrimarySubjects + languageSubjects)
      .distinctBy { it.id }

  val jssSubjects: List<com.example.domain.model.SubjectInfo> =
    (juniorSubjects + languageSubjects).distinctBy { it.id }

  fun subjectsForGrade(grade: String): List<com.example.domain.model.SubjectInfo> {
    if (grade.startsWith("Pre-Primary", ignoreCase = true)) return prePrimarySubjects
    val gNum = grade.filter { it.isDigit() }.toIntOrNull() ?: 7
    return when {
      gNum in 1..3 -> lowerPrimarySubjects
      gNum == 4 -> upperPrimarySubjects + languageSubjects.first { it.id == "arabic" }
      gNum in 5..6 -> upperPrimarySubjects
      gNum == 8 -> juniorSubjects + languageSubjects.first { it.id == "chinese" }
      gNum == 9 -> juniorSubjects + languageSubjects.first { it.id == "french" }
      else -> juniorSubjects
    }
  }

  fun subjectIdForArea(grade: String, areaOrId: String): String {
    val trimmed = areaOrId.trim()
    val all = primarySubjects + jssSubjects
    all.firstOrNull { it.id.equals(trimmed, ignoreCase = true) }?.let { return it.id }

    val gNum = grade.filter { it.isDigit() }.toIntOrNull() ?: 7
    val lower = trimmed.lowercase()
    return if (gNum in 1..6) {
      when {
        lower.contains("math") -> "math_p"
        lower.contains("eng") || lower.contains("literacy") -> "eng_p"
        lower.contains("kis") -> "kis_p"
        lower.contains("indig") -> "indig_p"
        lower.contains("arab") -> "arabic"
        lower.contains("chinese") || lower.contains("mandarin") -> "chinese"
        lower.contains("french") -> "french"
        lower.contains("agri") -> if (gNum >= 4) "agri" else "env_p"
        lower.contains("env") || lower.contains("sci") -> "env_p"
        lower.contains("soc") -> "sost"
        lower.contains("rel") || lower.contains("cre") || lower.contains("ire") -> "rel_p"
        lower.contains("creativ") || lower.contains("art") || lower.contains("music") -> "crt_p"
        else -> "math_p"
      }
    } else {
      when {
        lower.contains("eng") -> "eng"
        lower.contains("kis") -> "kis"
        lower.contains("math") -> "math"
        lower.contains("sci") -> "isci"
        lower.contains("soc") -> "sost"
        lower.contains("agri") -> "agri"
        lower.contains("indig") -> "indig"
        lower.contains("arab") -> "arabic"
        lower.contains("chinese") || lower.contains("mandarin") -> "chinese"
        lower.contains("french") -> "french"
        lower.contains("tech") -> "ptech"
        lower.contains("art") || lower.contains("sport") || lower.contains("creativ") -> "arts"
        lower.contains("rel") || lower.contains("cre") || lower.contains("ire") -> "reled"
        else -> "math"
      }
    }
  }

  /** Uses the catalogue's own subject id when present, falling back to the label. */
  fun subjectIdForDesign(grade: String, subjectId: String, areaOrId: String): String {
    val resolved = subjectId.trim().ifBlank { subjectIdForArea(grade, areaOrId) }
    val gradeNumber = grade.filter { it.isDigit() }.toIntOrNull() ?: 0
    // The catalogue uses indig_p and indig for the same junior-school subject.
    return if (gradeNumber >= 7 && resolved.equals("indig_p", ignoreCase = true)) "indig" else resolved
  }

  fun areaNameForSubjectId(subjectId: String): String =
    (primarySubjects + jssSubjects).firstOrNull { it.id.equals(subjectId.trim(), ignoreCase = true) }?.name
      ?: subjectId

  fun extractGradeFromStream(streamName: String): String {
    val ppMatch = Regex("\\b(PP\\s*[12])\\b", RegexOption.IGNORE_CASE).find(streamName)
    if (ppMatch != null) return ppMatch.groupValues[1].uppercase().replace(" ", "")
    val match = Regex("Grade\\s*(\\d+)", RegexOption.IGNORE_CASE).find(streamName)
    if (match != null) return "Grade ${match.groupValues[1]}"
    val digitMatch = Regex("\\b(10|[1-9])\\b").find(streamName)
    if (digitMatch != null) return "Grade ${digitMatch.groupValues[1]}"
    return cbcGrades.firstOrNull { streamName.contains(it, ignoreCase = true) } ?: "Grade 7"
  }

  fun levelForGrade(grade: String): String = when {
    grade.startsWith("Pre-Primary", ignoreCase = true) -> "Pre-Primary"
    grade == "Grade 1" || grade == "Grade 2" || grade == "Grade 3" -> "Lower Primary"
    grade == "Grade 4" || grade == "Grade 5" || grade == "Grade 6" -> "Upper Primary"
    else -> "Junior School"
  }

  val kicdDesigns: List<KicdDesignItem> = listOf(
    KicdDesignItem(
      id = "kicd_g1_math_1",
      level = "Lower Primary",
      grade = "Grade 1",
      learningArea = "Mathematical Activities",
      strand = "1.0 Numbers",
      subStrand = "1.1 Pre-Number Activities & Whole Numbers",
      suggestedLessons = 10,
      specificLearningOutcomes = listOf(
        "Sort and group objects according to attributes such as colour, size, and shape.",
        "Count numbers forward up to 100 and backward from 50.",
        "Read and write numbers 1 to 50 in symbols and 1 to 10 in words.",
      ),
      keyInquiryQuestions = listOf(
        "How can we group objects we use in our daily life?",
        "Why do we count things around us?",
      ),
      learningExperiences = listOf(
        "Learners collect locally available safe objects (bottle tops, sticks, stones) and sort them by colour and size in pairs.",
        "Learners sing number songs and use number cards to sequence numbers 1–50.",
      ),
      coreCompetencies = listOf("Critical Thinking and Problem Solving", "Communication and Collaboration"),
      values = listOf("Unity", "Responsibility", "Respect"),
      pcis = listOf("Environmental conservation (collecting safe local counters)", "Life skills"),
      suggestedResources = listOf("Number charts", "Bottle tops", "Beads", "Abacus", "KICD Grade 1 Mathematics Curriculum Design"),
      assessmentRubric = mapOf(
        "EE" to "Accurately sorts, counts up to 100, and writes numbers 1–50 independently and assists peers.",
        "ME" to "Correctly sorts objects and counts/writes numbers up to 50 with minimal prompts.",
        "AE" to "Sorts objects by one attribute and counts up to 30 with occasional teacher guidance.",
        "BE" to "Requires consistent support to sort objects and recognize number symbols 1–20.",
      ),
    ),
    KicdDesignItem(
      id = "kicd_g1_math_2",
      level = "Lower Primary",
      grade = "Grade 1",
      learningArea = "Mathematical Activities",
      strand = "1.0 Numbers",
      subStrand = "1.2 Addition & Subtraction (Single & Double Digit)",
      suggestedLessons = 12,
      specificLearningOutcomes = listOf(
        "Model addition as putting objects together and use the '+' and '=' signs.",
        "Add two single-digit numbers with sums up to 18 using concrete objects.",
        "Subtract single-digit numbers from numbers up to 20.",
      ),
      keyInquiryQuestions = listOf(
        "How do we get the total when we put two groups of items together?",
      ),
      learningExperiences = listOf(
        "Learners use counters and number lines in small groups to combine and take away items.",
        "Learners play digital or board number-bond games.",
      ),
      coreCompetencies = listOf("Critical Thinking and Problem Solving", "Digital Literacy"),
      values = listOf("Sharing", "Honesty", "Unity"),
      pcis = listOf("Financial literacy (counting coins)"),
      suggestedResources = listOf("Counters", "Number line strips", "Place value tins"),
      assessmentRubric = mapOf(
        "EE" to "Solves addition and subtraction tasks mentally and explains steps clearly.",
        "ME" to "Correctly adds and subtracts using counters or number lines.",
        "AE" to "Adds single digits accurately but needs prompts on subtraction.",
        "BE" to "Struggles to relate '+' and '-' symbols to combining or taking away.",
      ),
    ),
    KicdDesignItem(
      id = "kicd_g1_eng_1",
      level = "Lower Primary",
      grade = "Grade 1",
      learningArea = "English Language",
      strand = "1.0 Listening and Speaking",
      subStrand = "1.1 Greetings, Introductions & Polite Language",
      suggestedLessons = 6,
      specificLearningOutcomes = listOf(
        "Respond appropriately to greetings at different times of the day.",
        "Introduce self and peers using polite expressions ('please', 'thank you', 'excuse me').",
        "Pronounce target consonant and vowel sounds clearly.",
      ),
      keyInquiryQuestions = listOf(
        "Why is it important to greet people politely?",
        "How do we greet elders and friends at different times of the day?",
      ),
      learningExperiences = listOf(
        "Role-play morning, afternoon, and evening greetings in pairs.",
        "Listen to audio recordings and imitate polite phrases and phonetic sounds.",
      ),
      coreCompetencies = listOf("Communication and Collaboration", "Self-Efficacy"),
      values = listOf("Respect", "Love", "Peace"),
      pcis = listOf("Social cohesion and etiquette"),
      suggestedResources = listOf("Flashcards", "Audio player", "Picture storybooks"),
      assessmentRubric = mapOf(
        "EE" to "Uses varied polite expressions naturally with clear articulation in role-play.",
        "ME" to "Greets and responds accurately using appropriate polite language.",
        "AE" to "Uses basic greetings with prompts on time-of-day distinctions.",
        "BE" to "Hesitant in responding to greetings; needs modeling.",
      ),
    ),
    KicdDesignItem(
      id = "kicd_g1_lit_1",
      level = "Lower Primary",
      grade = "Grade 1",
      learningArea = "Literacy Activities",
      strand = "1.0 Reading & Phonemic Awareness",
      subStrand = "1.1 Letter Sounds, Blending & Picture Reading",
      suggestedLessons = 8,
      specificLearningOutcomes = listOf(
        "Recognize and articulate common letter sounds (/a/, /e/, /i/, /o/, /u/, /m/, /s/, /t/).",
        "Blend two and three letter sounds to read CVC words.",
        "Interpret simple illustrations in storybooks to predict story events.",
      ),
      keyInquiryQuestions = listOf(
        "How do letter sounds help us read new words?",
      ),
      learningExperiences = listOf(
        "Sound-matching games with letter cards and sand-tray tracing.",
        "Guided shared reading from big picture books.",
      ),
      coreCompetencies = listOf("Learning to Learn", "Communication and Collaboration"),
      values = listOf("Responsibility", "Patience"),
      pcis = listOf("Child rights & storytelling"),
      suggestedResources = listOf("Alphabet frieze", "Sand trays", "Phonics flashcards", "Big Books"),
      assessmentRubric = mapOf(
        "EE" to "Blends sounds fluently to read unfamiliar CVC words and retells picture stories.",
        "ME" to "Identifies target letter sounds and blends simple words accurately.",
        "AE" to "Recognizes letter sounds but needs support when blending.",
        "BE" to "Identifies fewer than half of taught letter sounds.",
      ),
    ),
    KicdDesignItem(
      id = "kicd_g1_env_1",
      level = "Lower Primary",
      grade = "Grade 1",
      learningArea = "Environmental Activities",
      strand = "1.0 Social Environment",
      subStrand = "1.1 Weather, Home & School Cleanliness",
      suggestedLessons = 8,
      specificLearningOutcomes = listOf(
        "Identify different weather conditions (sunny, rainy, cloudy, windy).",
        "Practice proper handwashing and waste disposal in the school compound.",
        "Demonstrate care for plants and domestic animals in the immediate environment.",
      ),
      keyInquiryQuestions = listOf(
        "How does the weather affect what we wear and do?",
        "How can we keep our classroom and school compound clean?",
      ),
      learningExperiences = listOf(
        "Nature walk around the school to observe weather and sort litter into bins.",
        "Draw weather symbols on a daily classroom weather chart.",
      ),
      coreCompetencies = listOf("Citizenship", "Critical Thinking and Problem Solving"),
      values = listOf("Responsibility", "Unity", "Love"),
      pcis = listOf("Environmental conservation", "Health & hygiene"),
      suggestedResources = listOf("Weather chart", "Sorting bins", "Soap & running water station"),
      assessmentRubric = mapOf(
        "EE" to "Explains weather patterns, models hygiene consistently, and leads clean-up tasks.",
        "ME" to "Identifies weather symbols and participates actively in environmental care.",
        "AE" to "Names common weather types with picture cues.",
        "BE" to "Needs repeated guidance to match weather and hygiene practices.",
      ),
    ),
    KicdDesignItem(
      id = "kicd_g1_kis_1",
      level = "Lower Primary",
      grade = "Grade 1",
      learningArea = "Kiswahili Language",
      strand = "1.0 Kusikiliza na Kuzungumza",
      subStrand = "1.1 Maamkuzi na Adabu (Shikamoo, Hujambo)",
      suggestedLessons = 6,
      specificLearningOutcomes = listOf(
        "Kutambua na kutumia maamkuzi mwafaka kulingana na umri na wakati.",
        "Kujibu maamkuzi kwa heshima katika mazungumzo ya kila siku.",
        "Kutaja majina ya vifaa vya darasani kwa Kiswahili sanifu.",
      ),
      keyInquiryQuestions = listOf(
        "Kwa nini tunasalimia wazazi na walimu kwa heshima?",
      ),
      learningExperiences = listOf(
        "Igizo dhima la kusalimiana shuleni na nyumbani katika vikundi.",
        "Kuimba nyimbo za maamkuzi na kutaja vifaa vya darasani.",
      ),
      coreCompetencies = listOf("Communication and Collaboration", "Citizenship"),
      values = listOf("Heshima (Respect)", "Upendo (Love)", "Umoja (Unity)"),
      pcis = listOf("Maadili mema na uraia mwema"),
      suggestedResources = listOf("Kadi za maneno", "Picha za vifaa vya darasani", "Kitabu cha Kiswahili"),
      assessmentRubric = mapOf(
        "EE" to "Anatumia maamkuzi na msamiati wa darasani kwa ufasaha na kuigiza kwa ubunifu.",
        "ME" to "Anaamkia na kujibu maamkuzi ipasavyo bila kusaidiwa.",
        "AE" to "Anaamkia kwa msaada wa mwalimu au wenzake.",
        "BE" to "Ana ugumu wa kutofautisha maamkuzi ya umri tofauti.",
      ),
    ),
    KicdDesignItem(
      id = "kicd_g1_cre_1",
      level = "Lower Primary",
      grade = "Grade 1",
      learningArea = "Creative Activities",
      strand = "1.0 Creating and Executing",
      subStrand = "1.1 Drawing, Pattern Making & Action Songs",
      suggestedLessons = 8,
      specificLearningOutcomes = listOf(
        "Draw simple shapes and lines using crayons and natural pigments.",
        "Sing simple Kenyan folk action songs with rhythmic clapping.",
        "Perform basic locomotor movements safely in open space.",
      ),
      keyInquiryQuestions = listOf(
        "What materials in our surroundings can we use to make patterns?",
      ),
      learningExperiences = listOf(
        "Leaf-printing and freehand crayon pattern drawing on recycled paper.",
        "Group singing games with clapping and coordinated stepping.",
      ),
      coreCompetencies = listOf("Creativity and Imagination", "Self-Efficacy"),
      values = listOf("Unity", "Joy", "Respect"),
      pcis = listOf("Safety during play", "Cultural heritage"),
      suggestedResources = listOf("Crayons", "Manila paper", "Leaves", "Percussion shakers"),
      assessmentRubric = mapOf(
        "EE" to "Creates intricate rhythmic patterns and leads peers in action songs.",
        "ME" to "Draws neat patterns and keeps steady beat during singing games.",
        "AE" to "Participates in drawing and singing with occasional prompts.",
        "BE" to "Requires physical and verbal guidance to complete patterns.",
      ),
    ),
    KicdDesignItem(
      id = "kicd_g1_rel_1",
      level = "Lower Primary",
      grade = "Grade 1",
      learningArea = "Religious Activities",
      strand = "1.0 Creation",
      subStrand = "1.1 Self-Awareness & God's Creation",
      suggestedLessons = 5,
      specificLearningOutcomes = listOf(
        "Appreciate self as uniquely created and loved by God.",
        "Name plants, animals, and heavenly bodies created by God.",
        "Demonstrate kindness and care toward classmates and family.",
      ),
      keyInquiryQuestions = listOf(
        "How should we take care of ourselves and God's creation?",
      ),
      learningExperiences = listOf(
        "Singing thanksgiving songs and drawing favourite plants and animals.",
        "Sharing stories about helping others at home.",
      ),
      coreCompetencies = listOf("Self-Efficacy", "Citizenship"),
      values = listOf("Love", "Responsibility", "Gratitude"),
      pcis = listOf("Environmental stewardship"),
      suggestedResources = listOf("Children's Bible / Religious storybook", "Drawing charts"),
      assessmentRubric = mapOf(
        "EE" to "Expresses deep appreciation of creation and models kindness consistently.",
        "ME" to "Identifies elements of creation and shares ways to care for them.",
        "AE" to "Names a few created things with visual cues.",
        "BE" to "Needs prompting to relate creation to daily care.",
      ),
    ),
    // Upper Primary (Grades 4-6)
    KicdDesignItem(
      id = "kicd_g4_sci_1",
      level = "Upper Primary",
      grade = "Grade 4",
      learningArea = "Science & Technology",
      strand = "1.0 Living Things",
      subStrand = "1.1 Plants & Animals: Characteristics and Care",
      suggestedLessons = 10,
      specificLearningOutcomes = listOf(
        "Identify characteristics of living things (feeding, breathing, movement, growth, reproduction).",
        "Classify local plants into flowering and non-flowering plants.",
        "Observe safety precautions when handling plants and small animals.",
      ),
      keyInquiryQuestions = listOf(
        "How do we tell that something is living or non-living?",
        "Why should we wear protective gear when handling unfamiliar plants?",
      ),
      learningExperiences = listOf(
        "Guided nature walk with magnifying glasses and gloves to record plant types.",
        "Use tablets/cameras to document flowering plants in a digital portfolio.",
      ),
      coreCompetencies = listOf("Critical Thinking and Problem Solving", "Digital Literacy"),
      values = listOf("Responsibility", "Respect"),
      pcis = listOf("Biodiversity conservation", "Personal safety"),
      suggestedResources = listOf("Hand lens", "Protective gloves", "School garden", "Science & Tech Grade 4 Learner's Book"),
      assessmentRubric = mapOf(
        "EE" to "Classifies diverse specimens accurately and explains safety rationale clearly.",
        "ME" to "Lists characteristics of living things and classifies local plants correctly.",
        "AE" to "Identifies basic living characteristics with occasional misconceptions.",
        "BE" to "Unable to distinguish living from non-living things without assistance.",
      ),
    ),
    KicdDesignItem(
      id = "kicd_g4_math_1",
      level = "Upper Primary",
      grade = "Grade 4",
      learningArea = "Mathematics",
      strand = "1.0 Numbers",
      subStrand = "1.1 Place Value, Rounding Off & Fractions",
      suggestedLessons = 12,
      specificLearningOutcomes = listOf(
        "Identify place value and total value of digits up to tens of thousands.",
        "Round off numbers up to 1,000 to the nearest ten.",
        "Represent unit fractions and compare fractions with the same denominator.",
      ),
      keyInquiryQuestions = listOf(
        "Where do we use rounding off and fractions in shopping or sharing?",
      ),
      learningExperiences = listOf(
        "Place value abacus stations and paper-folding fraction strips.",
        "Word problems drawn from market prices in Kenya Shillings.",
      ),
      coreCompetencies = listOf("Critical Thinking and Problem Solving", "Learning to Learn"),
      values = listOf("Integrity", "Fairness"),
      pcis = listOf("Financial literacy"),
      suggestedResources = listOf("Abacus", "Place value charts", "Fraction cut-outs"),
      assessmentRubric = mapOf(
        "EE" to "Applies place value and fraction comparisons to multi-step real-life problems.",
        "ME" to "Correctly determines place value, rounds numbers, and compares unit fractions.",
        "AE" to "Works out place value up to thousands; struggles with rounding.",
        "BE" to "Needs manipulatives to identify digits beyond hundreds.",
      ),
    ),
    // Junior School (Grades 7-9)
    KicdDesignItem(
      id = "kicd_g7_intsci_1",
      level = "Junior School",
      grade = "Grade 7",
      learningArea = "Integrated Science",
      strand = "1.0 Scientific Investigation",
      subStrand = "1.1 Laboratory Safety, Apparatus & Basic Units (SI)",
      suggestedLessons = 10,
      specificLearningOutcomes = listOf(
        "Identify common laboratory hazards and interpret hazard symbols.",
        "Demonstrate proper use and care of basic laboratory apparatus.",
        "Measure length, mass, time, and temperature using SI units accurately.",
      ),
      keyInquiryQuestions = listOf(
        "Why is standardizing measurement units important in science and trade?",
        "How do hazard symbols prevent accidents in a laboratory?",
      ),
      learningExperiences = listOf(
        "Practical station rotation measuring mass (beam balance), volume (measuring cylinder), and temperature.",
        "Design a classroom laboratory safety poster in collaborative teams.",
      ),
      coreCompetencies = listOf("Critical Thinking and Problem Solving", "Communication and Collaboration", "Self-Efficacy"),
      values = listOf("Responsibility", "Integrity"),
      pcis = listOf("Disaster risk reduction", "Safety and security"),
      suggestedResources = listOf("Measuring cylinders", "Thermometers", "Triple-beam balance", "Hazard symbol charts"),
      assessmentRubric = mapOf(
        "EE" to "Records precise SI measurements with zero parallax error and leads lab safety protocol.",
        "ME" to "Uses apparatus safely and converts basic SI units accurately.",
        "AE" to "Identifies major apparatus but makes minor reading or unit errors.",
        "BE" to "Requires supervision to handle apparatus and record units.",
      ),
    ),
    KicdDesignItem(
      id = "kicd_g7_pretech_1",
      level = "Junior School",
      grade = "Grade 7",
      learningArea = "Pre-Technical Studies",
      strand = "1.0 Foundations of Pre-Technical Studies",
      subStrand = "1.1 Technical Drawing, Safety & Computer Hardware",
      suggestedLessons = 10,
      specificLearningOutcomes = listOf(
        "Identify personal protective equipment (PPE) for workshop environments.",
        "Sketch orthographic and oblique geometric shapes using drawing instruments.",
        "Classify computer hardware into input, processing, output, and storage devices.",
      ),
      keyInquiryQuestions = listOf(
        "How does technical drawing help engineers and artisans build accurate products?",
      ),
      learningExperiences = listOf(
        "Freehand and instrument sketching of simple wooden/metal joints.",
        "Disassembly and labeling demonstration of computer components.",
      ),
      coreCompetencies = listOf("Creativity and Imagination", "Digital Literacy", "Critical Thinking and Problem Solving"),
      values = listOf("Responsibility", "Patriotism"),
      pcis = listOf("Safety in workplaces", "Technological innovation"),
      suggestedResources = listOf("Set squares", "Drawing boards", "Sample PPE", "Desktop hardware kit"),
      assessmentRubric = mapOf(
        "EE" to "Produces clean dimensioned sketches and explains hardware functions accurately.",
        "ME" to "Uses drawing instruments properly and classifies hardware devices.",
        "AE" to "Completes basic sketches with guidance on neatness and proportions.",
        "BE" to "Needs direct assistance with tools and safety rules.",
      ),
    ),
  )

  val approvedResources: List<ApprovedResource> = listOf(
    ApprovedResource("res_1", "KLB Visionary Mathematical Activities Grade 1", "Kenya Literature Bureau (KLB)", "Grade 1", "Mathematical Activities", "Learner's Book & Teacher's Guide", "KICD-2024-M1"),
    ApprovedResource("res_2", "Oxford New Progressive Primary English Grade 1", "Oxford University Press EA", "Grade 1", "English Language", "Learner's Workbook", "KICD-2024-E1"),
    ApprovedResource("res_3", "Longhorn Kiswahili Dadisi Gredi ya 1", "Longhorn Publishers", "Grade 1", "Kiswahili Language", "Kitabu cha Mwanafunzi", "KICD-2024-K1"),
    ApprovedResource("res_4", "Moran Environmental Activities Grade 1", "Moran Publishers", "Grade 1", "Environmental Activities", "Activity Book & Charts", "KICD-2024-ENV1"),
    ApprovedResource("res_5", "Spotlight Science & Technology Grade 4", "Spotlight Publishers", "Grade 4", "Science & Technology", "Learner's Book & Lab Guide", "KICD-2024-ST4"),
    ApprovedResource("res_6", "Mentor Mathematics Grade 4", "Mentor Publishing", "Grade 4", "Mathematics", "Learner's Book", "KICD-2024-M4"),
    ApprovedResource("res_7", "KLB Top Scholar Integrated Science Grade 7", "Kenya Literature Bureau (KLB)", "Grade 7", "Integrated Science", "Textbook & Practical Manual", "KICD-2025-IS7"),
    ApprovedResource("res_8", "Distinction Pre-Technical Studies Grade 7", "Distinction Educational", "Grade 7", "Pre-Technical Studies", "Learner's Book", "KICD-2025-PT7"),
    ApprovedResource("res_9", "Kenya Education Cloud (KEC) Interactive Portal", "KICD Digital Content", "All Grades", "All Learning Areas", "Digital Interactive", "KICD-KEC-2026"),
  )

  val seniorPathways: List<PathwayInfo> = listOf(
    PathwayInfo(
      id = "pathway_stem",
      name = "STEM (Science, Technology, Engineering & Mathematics)",
      tracks = listOf("Pure Sciences", "Applied Sciences", "Technical & Engineering", "Career & Technology Studies"),
      coreLearningAreas = listOf("Mathematics", "Integrated Science", "Pre-Technical Studies", "Agriculture & Nutrition"),
      careerExamples = listOf("Software Engineering", "Medicine & Health", "Civil Engineering", "Agri-Tech", "Aviation"),
    ),
    PathwayInfo(
      id = "pathway_social",
      name = "Social Sciences",
      tracks = listOf("Languages & Literature", "Humanities & Business Studies"),
      coreLearningAreas = listOf("English", "Kiswahili", "Social Studies", "Religious Education"),
      careerExamples = listOf("Law & Governance", "Economics & Finance", "Journalism", "Education", "Diplomacy"),
    ),
    PathwayInfo(
      id = "pathway_arts",
      name = "Arts & Sports Science",
      tracks = listOf("Performing Arts", "Visual & Applied Arts", "Sports Science"),
      coreLearningAreas = listOf("Creative Arts & Sports", "Physical Education", "Literacy Activities"),
      careerExamples = listOf("Graphic & Product Design", "Music Production", "Sports Coaching", "Film & Animation"),
    ),
  )

  /**
   * Lower Primary (Grades 1-3): 6 lessons + 2 morning breaks, dismisses at 1.00 PM.
   */
  val lowerPrimaryTimeSlots: List<Pair<Int, String>> = listOf(
    0 to "8.20 / 9.00",
    1 to "9.00 / 9.40",
    2 to "9.40 / 10.00",
    3 to "10.00 / 10.40",
    4 to "10.40 / 11.20",
    5 to "11.20 / 11.40",
    6 to "11.40 / 12.20",
    7 to "12.20 / 1.00",
  )

  /**
   * Upper Primary (Grades 4-6): 8 lessons + 2 morning breaks + Lunch break, ending at 3.20 PM.
   */
  val upperPrimaryTimeSlots: List<Pair<Int, String>> = listOf(
    0 to "8.20 / 9.00",
    1 to "9.00 / 9.40",
    2 to "9.40 / 10.00",
    3 to "10.00 / 10.40",
    4 to "10.40 / 11.20",
    5 to "11.20 / 11.40",
    6 to "11.40 / 12.20",
    7 to "12.20 / 1.00",
    8 to "1.00 / 2.00",
    9 to "2.00 / 2.40",
    10 to "2.40 / 3.20",
  )

  /**
   * Junior School (Grades 7-9): 9 lessons + 2 morning breaks + Lunch break, ending at 4.00 PM.
   */
  val juniorSchoolTimeSlots: List<Pair<Int, String>> = upperPrimaryTimeSlots + listOf(
    11 to "3.20 / 4.00",
  )

  val standardTimeSlots: List<Pair<Int, String>> = lowerPrimaryTimeSlots

  fun timeSlotsForGrade(grade: String): List<Pair<Int, String>> = when (levelForGrade(grade)) {
    "Lower Primary" -> lowerPrimaryTimeSlots
    "Upper Primary" -> upperPrimaryTimeSlots
    else -> juniorSchoolTimeSlots
  }

  fun timetableFormatTitle(grade: String): String = when (levelForGrade(grade)) {
    "Lower Primary" -> "LOWER CLASS TIMETABLE (8:20 AM – 1:00 PM)"
    "Upper Primary" -> "UPPER PRIMARY TIMETABLE (8:20 AM – 3:20 PM)"
    else -> "JUNIOR SCHOOL TIMETABLE (8:20 AM – 4:00 PM)"
  }

  fun defaultTimetableForGrade(grade: String): List<TimetableSlot> {
    val timeSlots = timeSlotsForGrade(grade)
    val grid: Map<String, List<String>> = when (grade) {
      "Grade 1", "Grade 2", "Grade 3" -> mapOf(
        "MON" to listOf("LITERACY ACTIVITIES", "ENGLISH LANGUAGE", "BREAK", "ENVIRONMENTAL ACTIVITIES", "ENGLISH LANGUAGE", "BREAK", "ENGLISH LANGUAGE", "ENVIRONMENTAL ACTIVITIES"),
        "TUE" to listOf("ENGLISH LANGUAGE", "MATHEMATICAL ACTIVITIES", "BREAK", "KISWAHILI LANGUAGE", "MATHEMATICAL ACTIVITIES", "BREAK", "MATHEMATICAL ACTIVITIES", "KISWAHILI LANGUAGE"),
        "WED" to listOf("RELIGIOUS ACTIVITIES", "CREATIVE ACTIVITIES", "BREAK", "ENVIRONMENTAL ACTIVITIES", "ENVIRONMENTAL ACTIVITIES", "BREAK", "KISWAHILI LANGUAGE", "MATHEMATICAL ACTIVITIES"),
        "THU" to listOf("ENVIRONMENTAL ACTIVITIES", "MATHEMATICAL ACTIVITIES", "BREAK", "CREATIVE ACTIVITIES", "ENVIRONMENTAL ACTIVITIES", "BREAK", "MATHEMATICAL ACTIVITIES", "KISWAHILI LANGUAGE"),
        "FRI" to listOf("CREATIVE ACTIVITIES", "MATHEMATICAL ACTIVITIES", "BREAK", "ENVIRONMENTAL ACTIVITIES", "CREATIVE ACTIVITIES", "BREAK", "MATHEMATICAL ACTIVITIES", "MATHEMATICAL ACTIVITIES"),
      )
      "Grade 4", "Grade 5", "Grade 6" -> mapOf(
        "MON" to listOf("MATHEMATICS", "ENGLISH", "BREAK", "SCIENCE & TECHNOLOGY", "KISWAHILI", "BREAK", "SOCIAL STUDIES", "CREATIVE ARTS", "LUNCH BREAK", "AGRICULTURE & NUTRITION", "PPI / LIBRARY"),
        "TUE" to listOf("ENGLISH", "MATHEMATICS", "BREAK", "AGRICULTURE & NUTRITION", "SCIENCE & TECHNOLOGY", "BREAK", "RELIGIOUS EDUCATION", "KISWAHILI", "LUNCH BREAK", "MATHEMATICS", "CREATIVE ARTS"),
        "WED" to listOf("KISWAHILI", "SOCIAL STUDIES", "BREAK", "MATHEMATICS", "CREATIVE ARTS", "BREAK", "ENGLISH", "SCIENCE & TECHNOLOGY", "LUNCH BREAK", "SOCIAL STUDIES", "GAMES / P.E."),
        "THU" to listOf("SCIENCE & TECHNOLOGY", "MATHEMATICS", "BREAK", "ENGLISH", "AGRICULTURE & NUTRITION", "BREAK", "KISWAHILI", "SOCIAL STUDIES", "LUNCH BREAK", "RELIGIOUS EDUCATION", "CLUBS & SOCIETIES"),
        "FRI" to listOf("CREATIVE ARTS", "RELIGIOUS EDUCATION", "BREAK", "MATHEMATICS", "ENGLISH", "BREAK", "SCIENCE & TECHNOLOGY", "AGRICULTURE & NUTRITION", "LUNCH BREAK", "KISWAHILI", "ASSEMBLIES / PASTORAL"),
      )
      else -> mapOf(
        "MON" to listOf("MATHEMATICS", "ENGLISH", "BREAK", "INTEGRATED SCIENCE", "PRE-TECHNICAL STUDIES", "BREAK", "SOCIAL STUDIES", "KISWAHILI", "LUNCH BREAK", "AGRICULTURE & NUTRITION", "CREATIVE ARTS & SPORTS", "PREP / GUIDED STUDY"),
        "TUE" to listOf("INTEGRATED SCIENCE", "MATHEMATICS", "BREAK", "KISWAHILI", "AGRICULTURE & NUTRITION", "BREAK", "CREATIVE ARTS & SPORTS", "ENGLISH", "LUNCH BREAK", "PRE-TECHNICAL STUDIES", "SOCIAL STUDIES", "GAMES & SPORTS"),
        "WED" to listOf("PRE-TECHNICAL STUDIES", "SOCIAL STUDIES", "BREAK", "MATHEMATICS", "RELIGIOUS EDUCATION", "BREAK", "INTEGRATED SCIENCE", "KISWAHILI", "LUNCH BREAK", "ENGLISH", "AGRICULTURE & NUTRITION", "SCIENCE LAB PRACTICUM"),
        "THU" to listOf("ENGLISH", "MATHEMATICS", "BREAK", "CREATIVE ARTS & SPORTS", "INTEGRATED SCIENCE", "BREAK", "PRE-TECHNICAL STUDIES", "SOCIAL STUDIES", "LUNCH BREAK", "RELIGIOUS EDUCATION", "KISWAHILI", "CLUBS & MENTORSHIP"),
        "FRI" to listOf("AGRICULTURE & NUTRITION", "KISWAHILI", "BREAK", "MATHEMATICS", "ENGLISH", "BREAK", "INTEGRATED SCIENCE", "RELIGIOUS EDUCATION", "LUNCH BREAK", "PRE-TECHNICAL STUDIES", "CREATIVE ARTS & SPORTS", "PASTORAL CARE"),
      )
    }

    val result = mutableListOf<TimetableSlot>()
    for ((day, subjects) in grid) {
      subjects.forEachIndexed { idx, subj ->
        val time = timeSlots[idx].second
        result.add(
          TimetableSlot(
            id = "${grade}_${day}_$idx",
            grade = grade,
            dayOfWeek = day,
            slotIndex = idx,
            timeRange = time,
            learningArea = subj,
            isBreak = subj == "BREAK" || subj == "LUNCH BREAK",
            room = "$grade Room",
          )
        )
      }
    }
    return result
  }
}
