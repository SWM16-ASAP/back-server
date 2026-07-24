# Seed data

현재는 구체적인 도메인 부하 시나리오가 범위 밖이므로 reset은 MongoDB, Redis, MySQL을 비우고 WireMock mapping만 다시 등록한다.

시나리오를 추가할 때는 `common/`에 모든 시나리오가 공유하는 최소 fixture를 두고, `scenarios/<scenario-name>/`에 해당 검증에 필요한 fixture만 둔다. 데이터는 결정적이고 멱등적으로 적용 가능해야 하며 운영 데이터 전체 복제본을 두지 않는다.
