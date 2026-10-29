package com.iafabric.gamita;

import java.util.Map;

/** The welcome's few texts in the mods' languages (by the game's language; English when it has none). */
final class GamitaTexts {
	// thanks, collection, satisfied, discord, patreon, perks, only mod, only port
	private static final Map<String, String[]> TEXTS = Map.ofEntries(
		Map.entry("en", new String[]{"Thanks for playing my mods!", "My mods in this game", "Satisfied!",
			"Ideas or bugs: Discord", "Support me: Patreon", "On Patreon: vote the next mod, try what's new first and make your idea real.",
			"This is the only GAMITA_MODELS mod you have.", "This is the only GAMITA_MODELS port you have."}),
		Map.entry("es", new String[]{"¡Gracias por jugar mis mods!", "Mis mods en este juego", "¡Satisfecho!",
			"Ideas o errores: Discord", "Apóyame: Patreon", "En Patreon: vota el próximo mod, prueba antes lo nuevo y haz realidad tu idea.",
			"Solo tienes este mod de GAMITA_MODELS.", "Solo tienes este port de GAMITA_MODELS."}),
		Map.entry("pt", new String[]{"Obrigado por jogar meus mods!", "Meus mods neste jogo", "Satisfeito!",
			"Ideias ou erros: Discord", "Me apoie: Patreon", "No Patreon: vote no próximo mod, teste as novidades antes e realize sua ideia.",
			"Este é o único mod da GAMITA_MODELS que você tem.", "Este é o único port da GAMITA_MODELS que você tem."}),
		Map.entry("fr", new String[]{"Merci de jouer à mes mods !", "Mes mods dans ce jeu", "Satisfait !",
			"Idées ou bugs : Discord", "Soutiens-moi : Patreon", "Sur Patreon : vote pour le prochain mod, teste les nouveautés en premier et réalise ton idée.",
			"C'est le seul mod de GAMITA_MODELS que tu as.", "C'est le seul portage de GAMITA_MODELS que tu as."}),
		Map.entry("de", new String[]{"Danke, dass du meine Mods spielst!", "Meine Mods in diesem Spiel", "Zufrieden!",
			"Ideen oder Fehler: Discord", "Unterstütze mich: Patreon", "Auf Patreon: stimme über den nächsten Mod ab, teste Neues zuerst und mach deine Idee wahr.",
			"Das ist der einzige Mod von GAMITA_MODELS, den du hast.", "Das ist der einzige Port von GAMITA_MODELS, den du hast."}),
		Map.entry("ru", new String[]{"Спасибо, что играешь в мои моды!", "Мои моды в этой игре", "Доволен!",
			"Идеи или ошибки: Discord", "Поддержи меня: Patreon", "На Patreon: голосуй за следующий мод, пробуй новое первым и воплоти свою идею.",
			"Это единственный мод GAMITA_MODELS у тебя.", "Это единственный порт GAMITA_MODELS у тебя."}),
		Map.entry("uk", new String[]{"Дякую, що граєш у мої моди!", "Мої моди в цій грі", "Задоволений!",
			"Ідеї чи помилки: Discord", "Підтримай мене: Patreon", "На Patreon: голосуй за наступний мод, пробуй нове першим і втіли свою ідею.",
			"Це єдиний мод GAMITA_MODELS у тебе.", "Це єдиний порт GAMITA_MODELS у тебе."}),
		Map.entry("pl", new String[]{"Dzięki za granie w moje mody!", "Moje mody w tej grze", "Zadowolony!",
			"Pomysły lub błędy: Discord", "Wesprzyj mnie: Patreon", "Na Patreonie: głosuj na następny mod, testuj nowości pierwszy i spełnij swój pomysł.",
			"To jedyny mod GAMITA_MODELS, jaki masz.", "To jedyny port GAMITA_MODELS, jaki masz."}),
		Map.entry("zh", new String[]{"感谢游玩我的模组！", "本游戏中的我的模组", "满意！",
			"想法或错误：Discord", "支持我：Patreon", "在 Patreon：投票决定下一个模组，抢先体验新内容，让你的想法成真。",
			"这是你唯一的 GAMITA_MODELS 模组。", "这是你唯一的 GAMITA_MODELS 移植版。"}),
		Map.entry("ja", new String[]{"私のMODを遊んでくれてありがとう！", "このゲームにある私のMOD", "満足！",
			"アイデアやバグ：Discord", "応援する：Patreon", "Patreonでは：次のMODに投票し、新要素を先に試し、あなたのアイデアを形にできます。",
			"これはあなたが持つ唯一のGAMITA_MODELSのMODです。", "これはあなたが持つ唯一のGAMITA_MODELSの移植版です。"}),
		Map.entry("ko", new String[]{"내 모드를 플레이해 줘서 고마워요!", "이 게임의 내 모드", "만족!",
			"아이디어나 버그: Discord", "후원하기: Patreon", "Patreon에서: 다음 모드에 투표하고, 새 기능을 먼저 해보고, 아이디어를 실현하세요.",
			"이것이 당신이 가진 유일한 GAMITA_MODELS 모드입니다.", "이것이 당신이 가진 유일한 GAMITA_MODELS 포트입니다."}),
		Map.entry("vi", new String[]{"Cảm ơn bạn đã chơi mod của tôi!", "Mod của tôi trong game này", "Hài lòng!",
			"Ý tưởng hoặc lỗi: Discord", "Ủng hộ tôi: Patreon", "Trên Patreon: bình chọn mod tiếp theo, thử điều mới trước và biến ý tưởng thành hiện thực.",
			"Đây là mod duy nhất của GAMITA_MODELS bạn có.", "Đây là bản port duy nhất của GAMITA_MODELS bạn có."}));

	static final int THANKS = 0, COLLECTION = 1, SATISFIED = 2, DISCORD = 3, PATREON = 4, PERKS = 5, ONLY_MOD = 6, ONLY_PORT = 7;

	private GamitaTexts() {
	}

	static String get(int which) {
		String code = GamitaCollection.mc().getLanguageManager().getSelected();
		String[] texts = TEXTS.getOrDefault(code.length() >= 2 ? code.substring(0, 2) : code, TEXTS.get("en"));
		return texts[which];
	}
}
