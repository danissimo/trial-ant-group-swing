create table employee (
	id identity,
	name varchar(64) not null unique check(length(name) > 0 and name = trim(name))
);

create table attendance (
	id identity,
	employee_id bigint not null references employee(id),
	checked_in timestamp not null,
	checked_out timestamp,
	check(checked_out >= checked_in)
);
create index on attendance(checked_in);
create index on attendance(checked_out);

insert into employee(name) values('Абрашитов Влад');
insert into employee(name) values('Адилов Ильяс');
insert into employee(name) values('Аксенова Елизавета');
insert into employee(name) values('Бакланова Наталья');
insert into employee(name) values('Блюменкранс Анастасия');
insert into employee(name) values('Бордошенко Эрнест');
insert into employee(name) values('Волкова Юлия');
insert into employee(name) values('Воробьева Анна');
insert into employee(name) values('Высоцкий Олег');
insert into employee(name) values('Габидуллин Эдуард');
insert into employee(name) values('Газизова Наилюша');
insert into employee(name) values('Галеев Марсель');
insert into employee(name) values('Гибова Оксана');
insert into employee(name) values('Гимадисламов Ильдар');
insert into employee(name) values('Гирфанов Николай');
insert into employee(name) values('Глобин Артём');
insert into employee(name) values('Губко Александр');
insert into employee(name) values('Гудзоватый Александр');
insert into employee(name) values('Дмитриева Мария');
insert into employee(name) values('Дубинина Жанна');
insert into employee(name) values('Елисеев Роман');
insert into employee(name) values('Жукова Юлия');
insert into employee(name) values('Зайцев Андрей');
insert into employee(name) values('Иванова Марьяна');
insert into employee(name) values('Игнатов Игорь');
insert into employee(name) values('Кадырова Виолетта');
insert into employee(name) values('Казакова Мира');
insert into employee(name) values('Камалова Мария');
insert into employee(name) values('Камилова Юлия');
insert into employee(name) values('Карабанова Валентина');
insert into employee(name) values('Ким Надюша');
insert into employee(name) values('Красавина Мари');
insert into employee(name) values('Краснова Мария');
insert into employee(name) values('Криницин Руслан');
insert into employee(name) values('Круть Рада');
insert into employee(name) values('Ласкова Настя');
insert into employee(name) values('Лифинова Екатерина');
insert into employee(name) values('Мадолимов Диловар');
insert into employee(name) values('Малахова Аленочка');
insert into employee(name) values('Малиновская Виктория');
insert into employee(name) values('Мензатов Энчик');
insert into employee(name) values('Момо Ничка');
insert into employee(name) values('Плужная Елизавета');
insert into employee(name) values('Полякова Елена');
insert into employee(name) values('Птицына Анастасия');
insert into employee(name) values('Пустильник Мария');
insert into employee(name) values('Рослякова Надежда');
insert into employee(name) values('Смертин Вася');
insert into employee(name) values('Соболевская Вероника');
insert into employee(name) values('Сосновская Викуленька');
insert into employee(name) values('Софиян Рома');
insert into employee(name) values('Ставицкая Милана');
insert into employee(name) values('Стецко Анютка');
insert into employee(name) values('Стрелка Ольга');
insert into employee(name) values('Стрижов Гена');
insert into employee(name) values('Тарасова Ольга');
insert into employee(name) values('Ткаченко Маргарита');
insert into employee(name) values('Туйчиев Заруф');
insert into employee(name) values('Туктагулова Гузель');
insert into employee(name) values('Тё Игорь');
insert into employee(name) values('Узнавенко Татьяна');
insert into employee(name) values('Федоров Денис');
insert into employee(name) values('Федотов Илья');
insert into employee(name) values('Фролова Светлана');
insert into employee(name) values('Хабибрахманова Дарина');
insert into employee(name) values('Хиргий Виктория');
insert into employee(name) values('Цветкова Елена');
insert into employee(name) values('Чигирев Александр');
insert into employee(name) values('Чижкова Елизавета');
insert into employee(name) values('Шалопай Тима');
insert into employee(name) values('Шупкина Ирина');
