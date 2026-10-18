
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

async function getFortuneResult() {
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

document.querySelectorAll("img").forEach((img) => {
    img.setAttribute("draggable", "false");
});

document.addEventListener("contextmenu", (event) => {
    event.preventDefault();
});

document.addEventListener("dragstart", (event) => {
    event.preventDefault();
});

document.addEventListener("selectstart", (event) => {
    if (!event.target.closest("input, select, textarea")) {
        event.preventDefault();
    }
});

