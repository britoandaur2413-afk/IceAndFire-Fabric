package com.iafabric.gamita;

import java.util.Map;

/** The welcome's few texts in the mods' languages (by the game's language; English when it has none). */
final class GamitaTexts {
	// thanks, collection, satisfied, discord, patreon, future expansion, only mod, only port, no expansion yet
	private static final Map<String, String[]> TEXTS = Map.ofEntries(
		Map.entry("en", new String[]{"Thanks for playing my mods!", "My mods in this game", "Satisfied!",
			"Ideas or bugs: Discord", "Support the project and be among the first to try it", "FUTURE EXPANSION",
			"This is the only GAMITA_MODELS mod you have.", "This is the only GAMITA_MODELS port you have.",
			"No expansion planned for this mod yet: send your ideas on Discord. When there is one, support it on Patreon and be among the first to try it."}),
		Map.entry("es", new String[]{"¡Gracias por jugar mis mods!", "Mis mods en este juego", "¡Satisfecho!",
			"Ideas o errores: Discord", "Apoya el proyecto y sé de los primeros en probarlo", "FUTURA EXPANSIÓN",
			"Solo tienes este mod de GAMITA_MODELS.", "Solo tienes este port de GAMITA_MODELS.",
			"Por ahora no hay una expansión pensada para este mod: manda tus ideas al Discord. Cuando la haya, apóyala en Patreon y sé de los primeros en probarla."}),
		Map.entry("pt", new String[]{"Obrigado por jogar meus mods!", "Meus mods neste jogo", "Satisfeito!",
			"Ideias ou erros: Discord", "Apoie o projeto e seja dos primeiros a testar", "FUTURA EXPANSÃO",
			"Este é o único mod da GAMITA_MODELS que você tem.", "Este é o único port da GAMITA_MODELS que você tem.",
			"Ainda não há uma expansão planejada para este mod: mande suas ideias no Discord. Quando houver, apoie no Patreon e seja dos primeiros a testá-la."}),
		Map.entry("fr", new String[]{"Merci de jouer à mes mods !", "Mes mods dans ce jeu", "Satisfait !",
			"Idées ou bugs : Discord", "Soutiens le projet et sois parmi les premiers à l’essayer", "FUTURE EXTENSION",
			"C'est le seul mod de GAMITA_MODELS que tu as.", "C'est le seul portage de GAMITA_MODELS que tu as.",
			"Pas encore d’extension prévue pour ce mod : envoie tes idées sur Discord. Quand il y en aura une, soutiens-la sur Patreon et sois parmi les premiers à l’essayer."}),
		Map.entry("de", new String[]{"Danke, dass du meine Mods spielst!", "Meine Mods in diesem Spiel", "Zufrieden!",
			"Ideen oder Fehler: Discord", "Unterstütze das Projekt und teste es als Erster", "KOMMENDE ERWEITERUNG",
			"Das ist der einzige Mod von GAMITA_MODELS, den du hast.", "Das ist der einzige Port von GAMITA_MODELS, den du hast.",
			"Für diesen Mod ist noch keine Erweiterung geplant: schick deine Ideen auf Discord. Sobald es eine gibt, unterstütze sie auf Patreon und teste sie als Erster."}),
		Map.entry("ru", new String[]{"Спасибо, что играешь в мои моды!", "Мои моды в этой игре", "Доволен!",
			"Идеи или ошибки: Discord", "Поддержи проект и попробуй его одним из первых", "БУДУЩЕЕ РАСШИРЕНИЕ",
			"Это единственный мод GAMITA_MODELS у тебя.", "Это единственный порт GAMITA_MODELS у тебя.",
			"Для этого мода пока не запланировано расширение: присылай идеи в Discord. Когда оно появится, поддержи его на Patreon и попробуй одним из первых."}),
		Map.entry("uk", new String[]{"Дякую, що граєш у мої моди!", "Мої моди в цій грі", "Задоволений!",
			"Ідеї чи помилки: Discord", "Підтримай проєкт і спробуй його одним із перших", "МАЙБУТНЄ РОЗШИРЕННЯ",
			"Це єдиний мод GAMITA_MODELS у тебе.", "Це єдиний порт GAMITA_MODELS у тебе.",
			"Для цього моду поки не заплановано розширення: надсилай ідеї в Discord. Коли воно з’явиться, підтримай його на Patreon і спробуй одним із перших."}),
		Map.entry("pl", new String[]{"Dzięki za granie w moje mody!", "Moje mody w tej grze", "Zadowolony!",
			"Pomysły lub błędy: Discord", "Wesprzyj projekt i wypróbuj go jako jeden z pierwszych", "PRZYSZŁE ROZSZERZENIE",
			"To jedyny mod GAMITA_MODELS, jaki masz.", "To jedyny port GAMITA_MODELS, jaki masz.",
			"Na razie nie ma planowanego rozszerzenia tego moda: wysyłaj pomysły na Discordzie. Gdy będzie, wesprzyj je na Patreonie i wypróbuj jako jeden z pierwszych."}),
		Map.entry("zh", new String[]{"感谢游玩我的模组！", "本游戏中的我的模组", "满意！",
			"想法或错误：Discord", "支持这个项目，抢先体验", "未来扩展",
			"这是你唯一的 GAMITA_MODELS 模组。", "这是你唯一的 GAMITA_MODELS 移植版。",
			"这个模组暂时还没有计划中的扩展：欢迎把想法发到 Discord。等有了扩展，可以在 Patreon 支持并抢先体验。"}),
		Map.entry("ja", new String[]{"私のMODを遊んでくれてありがとう！", "このゲームにある私のMOD", "満足！",
			"アイデアやバグ：Discord", "プロジェクトを応援して、いち早く遊ぼう", "今後の拡張",
			"これはあなたが持つ唯一のGAMITA_MODELSのMODです。", "これはあなたが持つ唯一のGAMITA_MODELSの移植版です。",
			"このMODの拡張はまだ予定されていません。アイデアはDiscordへ。予定ができたら、Patreonで応援していち早く遊べます。"}),
		Map.entry("ko", new String[]{"내 모드를 플레이해 줘서 고마워요!", "이 게임의 내 모드", "만족!",
			"아이디어나 버그: Discord", "프로젝트를 후원하고 가장 먼저 플레이하세요", "향후 확장",
			"이것이 당신이 가진 유일한 GAMITA_MODELS 모드입니다.", "이것이 당신이 가진 유일한 GAMITA_MODELS 포트입니다.",
			"이 모드의 확장은 아직 계획되지 않았어요. 아이디어는 Discord로 보내 주세요. 확장이 생기면 Patreon에서 후원하고 가장 먼저 해 보세요."}),
		Map.entry("vi", new String[]{"Cảm ơn bạn đã chơi mod của tôi!", "Mod của tôi trong game này", "Hài lòng!",
			"Ý tưởng hoặc lỗi: Discord", "Ủng hộ dự án và là người chơi thử đầu tiên", "BẢN MỞ RỘNG SẮP TỚI",
			"Đây là mod duy nhất của GAMITA_MODELS bạn có.", "Đây là bản port duy nhất của GAMITA_MODELS bạn có.",
			"Mod này chưa có bản mở rộng nào được lên kế hoạch: hãy gửi ý tưởng lên Discord. Khi có, hãy ủng hộ trên Patreon và là người chơi thử đầu tiên."}));

	static final int THANKS = 0, COLLECTION = 1, SATISFIED = 2, DISCORD = 3, PATREON = 4, NEXT = 5, ONLY_MOD = 6, ONLY_PORT = 7, NONE = 8;

	private GamitaTexts() {
	}

	static String get(int which) {
		String code = GamitaCollection.mc().getLanguageManager().getSelected();
		String[] texts = TEXTS.getOrDefault(code.length() >= 2 ? code.substring(0, 2) : code, TEXTS.get("en"));
		return texts[which];
	}
}
