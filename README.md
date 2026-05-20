# CRRPGCore

Paper 서버를 위한 Kotlin 기반 RPG 코어 플러그인입니다.  
[CRFramework](https://github.com/zlero7/CRFramework) 위에서 동작합니다.

## 지원 버전

| 항목 | 버전 |
|------|------|
| Minecraft | 1.20.4 |
| Paper API | 1.20.4-R0.1-SNAPSHOT |
| Java | 21 |
| Kotlin | 2.3.10 |
| CRFramework | v1.0.3 |

## 기능

- **레벨 / 경험치 시스템** — 바닐라 XP를 커스텀 레벨로 전환, 레벨업 타이틀·사운드 지원
- **스텟 시스템** — 힘(데미지) / 체력(최대 HP) / 민첩(회피율) 스텟 포인트 분배
- **RPG 아이템** — 무기·방어구에 등급(COMMON~LEGENDARY) 및 기본 데미지 설정
- **강화 시스템** — +1~+10 강화, 강화석 종류 / 파괴·하락 방지권, 강화 GUI
- **감정(Appraisal) 시스템** — 소켓 기반 랜덤 스텟 부여, 재감정 지원
- **각성(Socket) 시스템** — 각성석으로 슬롯 생성, 재각성 지원
- **보석(Jewel) 시스템** — 보석 감정 및 루(Roon) 슬롯 장착, 9종 스텟 효과
- **경험치 부스트** — 개인/전체 XP 배수 스크롤, 액션바에 잔여 시간 표시
- **전투 통합** — RPG 무기 데미지, 치명타, 관통, 흡혈, 방어, 회피 계산
- **아이템 내구도 무한** — RPG 등급 아이템 내구도 감소 차단
- **전투 메시지 토글** — 공격/피격 전투 메시지 config로 ON/OFF
- **Vault Economy 연동** — 감정·각성 비용 차감

## 의존성

### 서버 플러그인 (plugins/ 폴더)
| 플러그인 | 필수 여부 |
|---------|---------|
| [CRFramework](https://github.com/zlero7/CRFramework) | ✅ 필수 |
| [Vault](https://github.com/MilkBowl/Vault) | ✅ 필수 |
| [CRGuild](https://github.com/zlero7/CRGuild) | ⚠️ 선택 (액션바 길드명 표시) |

## 설치

1. `CRFramework.jar`, `Vault.jar`를 서버 `plugins/` 폴더에 추가
2. `CRRPGCore.jar`를 `plugins/` 폴더에 추가
3. 서버 재시작
4. `plugins/CRRPGCore/config.yml` 설정 후 `/rpgcore reload`

## 빌드

```bash
git clone https://github.com/zlero7/CRRPGCore.git
cd CRRPGCore
./gradlew shadowJar
# build/libs/CRRPGCore-1.0.0.jar 생성
```

## 명령어

| 명령어 | 설명 | 권한 |
|-------|------|------|
| `/rpgcore level info [플레이어]` | 레벨/XP 정보 조회 | 없음 |
| `/rpgcore level setlevel <플레이어> <레벨>` | 레벨 설정 | `crrpgcore.admin` |
| `/rpgcore level setxp <플레이어> <xp>` | XP 설정 | `crrpgcore.admin` |
| `/rpgcore level givexp <플레이어> <xp>` | XP 지급 | `crrpgcore.admin` |
| `/rpgcore level xpboost <배수> <분> <개인\|전체>` | 부스트 스크롤 지급 | `crrpgcore.admin` |
| `/rpgcore stat` | 스텟 GUI 열기 | 없음 |
| `/rpgcore stat info [플레이어]` | 스텟 정보 조회 | 없음 |
| `/rpgcore stat reset <플레이어>` | 스텟 초기화 | `crrpgcore.admin` |
| `/rpgcore weapon rank <등급>` | 손에 든 아이템을 RPG 무기로 설정 | `crrpgcore.admin` |
| `/rpgcore weapon damage <값>` | 무기 기본 데미지 설정 | `crrpgcore.admin` |
| `/rpgcore armor rank <등급>` | 손에 든 아이템을 RPG 방어구로 설정 | `crrpgcore.admin` |
| `/rpgcore upgrade` | 강화 GUI 열기 | 없음 |
| `/rpgcore upgrade 강화석 <low\|mid\|high> [수] [플레이어]` | 강화석 지급 | `crrpgcore.admin` |
| `/rpgcore upgrade 파괴방어 [수] [플레이어]` | 파괴방지권 지급 | `crrpgcore.admin` |
| `/rpgcore upgrade 하락방어 [수] [플레이어]` | 하락방지권 지급 | `crrpgcore.admin` |
| `/rpgcore awake` | 각성/감정 GUI 열기 | 없음 |
| `/rpgcore awake scroll\|stone [플레이어] [수]` | 스크롤/각성석 지급 | `crrpgcore.admin` |
| `/rpgcore roon` | 루 장착 GUI 열기 | 없음 |
| `/rpgcore jewelry <등급> [수] [플레이어]` | 보석 지급 | `crrpgcore.admin` |
| `/rpgcore info [플레이어]` | 종합 정보 조회 | 없음 |
| `/rpgcore reload` | 설정 리로드 | `crrpgcore.admin` |
| `/스텟` | 스텟 GUI 열기 | 없음 |
| `/리롤설정 <각성\|감정> <횟수\|-1>` | 아이템 리롤 횟수 설정 | `crrpgcore.admin` |

## 아이템 등급

| 등급 | ID |
|------|----|
| 일반 | `COMMON` |
| 고급 | `UNCOMMON` |
| 희귀 | `RARE` |
| 영웅 | `EPIC` |
| 유일 | `UNIQUE` |
| 전설 | `LEGENDARY` |

## config.yml 주요 설정

```yaml
# 레벨
level:
  max-level: 100
  base-xp: 100
  multiplier: 1.2

# 스텟
stat:
  points-per-level: 3
  max-strength: 100
  max-vitality: 100
  max-agility: 100

# 경제 (Vault)
economy:
  socket-cost: 10000
  appraisal-cost: 30000
  socket-reroll-cost: 10000
  appraisal-reroll-cost: 30000

# 전투 메시지 표시 여부
combat:
  show-out-message: true   # 공격 시 데미지 메시지
  show-in-message: true    # 피격 시 데미지 메시지
```

## 아키텍처

CRFramework의 DI 컨테이너를 기반으로 동작합니다.

```
CRRPGCorePlugin (CRPlugin)
├── components() — DI 등록 목록
├── onCREnabled() — 초기화 (inject<>() 로 인스턴스 취득)
└── onCRDisabled() — 종료 (플레이어 데이터 저장)

Managers (생성자 주입)
├── LevelManager       레벨·XP 관리
├── StatManager        스텟 포인트 관리
├── RpgItemManager     RPG 아이템 타입·스텟 관리
├── UpgradeManager     강화 로직
├── AppraisalManager   감정 로직 (RollResult sealed class)
├── SocketManager      각성 슬롯 관리
├── JewelManager       보석·루 슬롯 관리
├── ArmorHealthManager 장비 HP AttributeModifier 관리
├── ActionBarManager   액션바 갱신 (CRScheduler)
├── PlayerDataManager  플레이어 데이터 YAML/MySQL 저장
├── XpBoostManager     XP 부스트 상태 관리
└── MessageConfig      config.yml 메시지 관리

Listeners (@Subscribe 자동 등록)
├── LevelListener
├── StatListener
├── RpgItemListener
├── RpgDurabilityListener
└── PlayerSessionListener
```

## 라이선스

MIT
