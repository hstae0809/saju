
const selectScreen = document.querySelector("#select_screen");
const inputScreen = document.querySelector("#input_screen");
const resultScreen = document.querySelector("#result_screen");

const characterButtons = document.querySelectorAll(
    "#select_screen .character"
);

const backButton = document.querySelector("#back_to_select");
const selectedCharacterImage = document.querySelector("#selected_character_img");
const inputTitle = document.querySelector("#input_title");

const birthForm = document.querySelector("#birth_form");
const birthHour = document.querySelector("#birth_hour");
const birthMinute = document.querySelector("#birth_minute");
const unknownTime = document.querySelector("#unknown_time");
const leapMonthOption = document.querySelector("#leap_month_option");
const formMessage = document.querySelector("#form_message");

const resultFrame = document.querySelector("#result_frame");
const resultTitle = document.querySelector("#result_title");
const resultDescription = document.querySelector("#result_description");
const resultDetails = document.querySelector("#result_details");
const resultAction = document.querySelector("#result_action");
const resultItem = document.querySelector("#result_item");
const resultColor = document.querySelector("#result_color");
const resultReaction = document.querySelector("#result_reaction");
const resultQuote = document.querySelector("#result_quote");
const resultRetry = document.querySelector("#result_retry");

const fortuneState = {
    character: null,
    birth: null,
};

const characters = {
    warrior: {
        name: "용사냥",
        reaction: "img/seleted_2.png",
    },
    mage: {
        name: "마법사냥",
        reaction: "img/seleted_3.png",
    },
    healer: {
        name: "치유냥",
        reaction: "img/seleted_6.png",
    },
};

const resultImages = {
    warrior: {
        good: "img/frame_good1.PNG",
        normal: "img/IMG_1140.PNG",
        bad: "img/IMG_1144.PNG",
    },
    mage: {
        good: "img/frame_good2.PNG",
        normal: "img/IMG_1142.PNG",
        bad: "img/IMG_1146.PNG",
    },
    healer: {
        good: "img/frame_good3.PNG",
        normal: "img/IMG_1141.PNG",
        bad: "img/IMG_1145.PNG",
    },
};

const defaultSources = new Map();

let selectedButton = null;
let isSwitching = false;

characterButtons.forEach((button) => {
    const image = button.querySelector(".character_img");
    const character = characters[button.dataset.character];

    defaultSources.set(button, image.getAttribute("src"));

    const reactionImage = new Image();
    reactionImage.src = character.reaction;

    button.addEventListener("click", () => {
        if (isSwitching) return;

        isSwitching = true;
        selectedButton = button;
        fortuneState.character = button.dataset.character;

        image.src = character.reaction;
        button.classList.add("is_selected");
        button.setAttribute("aria-pressed", "true");

        selectedCharacterImage.src = defaultSources.get(button);
        selectedCharacterImage.alt = character.name;

        window.setTimeout(() => {
            selectScreen.hidden = true;
            inputScreen.hidden = false;

            window.scrollTo(0, 0);
            inputTitle.focus({ preventScroll: true });

            isSwitching = false;
        }, 300);
    });
});

backButton.addEventListener("click", () => {
    inputScreen.hidden = true;
    selectScreen.hidden = false;

    if (selectedButton) {
        const image = selectedButton.querySelector(".character_img");

        image.src = defaultSources.get(selectedButton);

        selectedButton.classList.remove("is_selected");
        selectedButton.setAttribute("aria-pressed", "false");

        window.scrollTo(0, 0);
        selectedButton.focus({ preventScroll: true });
    }

    selectedButton = null;
    fortuneState.character = null;
    fortuneState.birth = null;
    formMessage.textContent = "";
});

for (let hour = 0; hour < 24; hour++) {
    const value = String(hour).padStart(2, "0");
    birthHour.add(new Option(`${value}시`, value));
}

for (let minute = 0; minute < 60; minute++) {
    const value = String(minute).padStart(2, "0");
    birthMinute.add(new Option(`${value}분`, value));
}

function updateTimeFields() {
    const isUnknown = unknownTime.checked;

    birthHour.disabled = isUnknown;
    birthMinute.disabled = isUnknown;

    birthHour.required = !isUnknown;
    birthMinute.required = !isUnknown;
}

unknownTime.addEventListener("change", updateTimeFields);
updateTimeFields();

function updateCalendarFields() {
    const isLunar = birthForm.elements.calendar.value === "lunar";

    leapMonthOption.hidden = !isLunar;

    if (!isLunar) {
        birthForm.elements.leap_month.checked = false;
    }
}

birthForm.querySelectorAll('[name="calendar"]').forEach((radio) => {
    radio.addEventListener("change", updateCalendarFields);
});

updateCalendarFields();

birthForm.addEventListener("input", () => {
    formMessage.textContent = "";
    fortuneState.birth = null;
});

const USE_MOCK = false;

const mockResults = [
    {
        grade: "good",
        title: "뜻밖의 반가움이 찾아오는 날",
        description: "평소 같으면 지나쳤을 곳에서 작은 즐거움을 발견할지도 몰라요.",
        details: [
            "궁금했던 일이나 망설였던 제안이 있다면 가볍게 한 걸음 내디뎌 보세요.",
            "거창한 도전보다 작은 시도가 즐거운 하루를 만들어줄 거예요."
        ],
        action: "궁금해서 저장만 해뒀던 것 하나를 해보세요.",
        luckyItem: "열쇠고리",
        luckyColor: "하늘색",
        quote: "작은 발견도 모이면 멋진 모험이 된다고!"
    },
    {
        grade: "normal",
        title: "평범함 속에서 발견하는 작은 행복",
        description: "오늘은 소소한 일상이 마음을 편안하게 해줄 거예요.",
        details: [
            "서두르지 않고 자신의 속도를 지켜보세요.",
            "익숙한 풍경에서도 새로운 기분을 발견할 수 있어요."
        ],
        action: "좋아하는 음악을 들으며 산책해 보세요.",
        luckyItem: "머그컵",
        luckyColor: "연두색",
        quote: "오늘은 천천히 가도 괜찮아!"
    },
    {
        grade: "bad",
        title: "조금은 신중하게 걸어야 하는 날",
        description: "생각했던 것과 다른 일이 생겨도 조급해하지 마세요.",
        details: [
            "오늘은 중요한 결정을 서두르지 않는 것이 좋겠어요.",
            "충분한 휴식으로 내일을 준비해 보세요."
        ],
        action: "따뜻한 음료를 마시며 잠깐 쉬어보세요.",
        luckyItem: "손수건",
        luckyColor: "보라색",
        quote: "모험도 좋지만 쉬어 가는 날도 필요하지!"
    }
];

function getMockFortune() {
    const birth = fortuneState.birth;
    const today = new Date();

    const key = [
        today.getFullYear(),
        today.getMonth() + 1,
        today.getDate(),
        fortuneState.character,
        birth.year,
        birth.month,
        birth.day,
        birth.calendar,
        birth.leapMonth,
        birth.hour,
        birth.minute
    ].join("-");

    let hash = 0;

    for (let i = 0; i < key.length; i++) {
        hash = (
            Math.imul(hash, 31) + key.charCodeAt(i)
        ) | 0;
    }

    const index = (hash >>> 0) % mockResults.length;

    return {
        ...mockResults[index],
        date: today.toLocaleDateString("sv-SE")
    };
}


async function getFortuneResult() {
    if (USE_MOCK) {
        return getMockFortune();
    }

    const response = await fetch("/api/fortune", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            character: fortuneState.character,
            birth: fortuneState.birth
        })
    });

    if (!response.ok) {
        throw new Error("운세 결과를 불러오지 못했습니다.");
    }

    return await response.json();
}

function fitResultTitle() {
    resultTitle.style.removeProperty("font-size");
    resultTitle.style.removeProperty("white-space");
}

function renderFortuneResult(result) {
    const character = fortuneState.character;
    const grade = result.grade;

    const imagePath = resultImages[character]?.[grade];

    if (!imagePath) {
        throw new Error("결과 이미지가 설정되지 않았습니다.");
    }

    resultFrame.src = imagePath;
    resultFrame.alt = "";

    resultTitle.textContent = result.title;
    resultDescription.textContent = result.description;

    resultDetails.replaceChildren();

    for (const sentence of result.details) {
        const paragraph = document.createElement("p");

        paragraph.textContent = sentence;
        resultDetails.appendChild(paragraph);
    }

    resultAction.textContent = result.action;

    resultItem.textContent = result.luckyItem;
    resultColor.textContent = result.luckyColor;

    resultReaction.src = characters[character].reaction;
    resultReaction.alt = characters[character].name;

    resultQuote.textContent = result.quote;

    inputScreen.hidden = true;
    resultScreen.hidden = false;

    window.scrollTo(0, 0);
    resultTitle.focus({ preventScroll: true });

    fitResultTitle();

    document.fonts.ready.then(() => {
        if (!resultScreen.hidden) {
            fitResultTitle();
        }
    });
}

birthForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const data = new FormData(birthForm);

    const yearText = String(data.get("year") ?? "").trim();
    const year = Number(yearText);
    const month = Number(data.get("month"));
    const day = Number(data.get("day"));
    const calendar = data.get("calendar");

    formMessage.textContent = "";
    fortuneState.birth = null;

    if (!/^[0-9]{4}$/.test(yearText) || year < 1) {
        formMessage.textContent = "태어난 연도를 숫자 4자리로 확인해주세요.";
        birthForm.elements.year.focus();
        return;
    }

    if (!Number.isInteger(month) || month < 1 || month > 12) {
        formMessage.textContent = "월은 1월부터 12월까지 입력해주세요.";
        birthForm.elements.month.focus();
        return;
    }

    const maxDay = calendar === "lunar" ? 30 : 31;

    if (!Number.isInteger(day) || day < 1 || day > maxDay) {
        formMessage.textContent =
            `일은 1일부터 ${maxDay}일까지 입력해주세요.`;

        birthForm.elements.day.focus();
        return;
    }

    if (calendar === "solar") {
        const date = new Date(0);

        date.setFullYear(year, month - 1, day);
        date.setHours(0, 0, 0, 0);

        const isValidDate =
            date.getFullYear() === year &&
            date.getMonth() === month - 1 &&
            date.getDate() === day;

        if (!isValidDate) {
            formMessage.textContent =
                "존재하지 않는 날짜예요. 생년월일을 확인해주세요.";

            birthForm.elements.day.focus();
            return;
        }

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        if (date > today) {
            formMessage.textContent =
                "생년월일은 오늘까지의 날짜로 입력해주세요.";

            birthForm.elements.year.focus();
            return;
        }
    }

    fortuneState.birth = {
        year,
        month,
        day,
        calendar,
        leapMonth: calendar === "lunar" && data.has("leap_month"),
        unknownTime: unknownTime.checked,
        hour: unknownTime.checked ? null : Number(data.get("hour")),
        minute: unknownTime.checked ? null : Number(data.get("minute"))
    };

    try {
        formMessage.textContent = "운세를 불러오는 중이에요.";

        const result = await getFortuneResult();

        renderFortuneResult(result);

        formMessage.textContent = "";
    } catch (error) {
        console.error(error);

        formMessage.textContent =
            "운세를 불러오지 못했어요. 다시 시도해주세요.";
    }
});

resultRetry.addEventListener("click", () => {
    resultScreen.hidden = true;
    inputScreen.hidden = false;

    formMessage.textContent = "";

    window.scrollTo(0, 0);
    inputTitle.focus({ preventScroll: true });
});

window.addEventListener("resize", () => {
    if (!resultScreen.hidden) {
        fitResultTitle();
    }
});
