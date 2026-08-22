--
-- PostgreSQL database dump
--

-- Dumped from database version 17.2 (Postgres.app)
-- Dumped by pg_dump version 17.2 (Postgres.app)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: authors; Type: TABLE; Schema: public; Owner: david
--

CREATE TABLE public.authors (
    id integer NOT NULL,
    full_name character varying(100) NOT NULL
);


ALTER TABLE public.authors OWNER TO david;

--
-- Name: authors_id_seq; Type: SEQUENCE; Schema: public; Owner: david
--

CREATE SEQUENCE public.authors_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.authors_id_seq OWNER TO david;

--
-- Name: authors_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: david
--

ALTER SEQUENCE public.authors_id_seq OWNED BY public.authors.id;


--
-- Name: books; Type: TABLE; Schema: public; Owner: david
--

CREATE TABLE public.books (
    id integer NOT NULL,
    title character varying(100) NOT NULL,
    pages integer NOT NULL,
    author_id integer NOT NULL,
    series_id integer,
    status character varying(20) NOT NULL,
    start_date date,
    end_date date,
    current_page integer,
    isbn_13 character varying(13) NOT NULL,
    s3_url character varying,
    rating smallint,
    CONSTRAINT status_check CHECK (((status)::text = ANY ((ARRAY['UNREAD'::character varying, 'READ'::character varying])::text[])))
);


ALTER TABLE public.books OWNER TO david;

--
-- Name: books_id_seq; Type: SEQUENCE; Schema: public; Owner: david
--

CREATE SEQUENCE public.books_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.books_id_seq OWNER TO david;

--
-- Name: books_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: david
--

ALTER SEQUENCE public.books_id_seq OWNED BY public.books.id;


--
-- Name: series; Type: TABLE; Schema: public; Owner: david
--

CREATE TABLE public.series (
    id integer NOT NULL,
    name character varying(100) NOT NULL
);


ALTER TABLE public.series OWNER TO david;

--
-- Name: series_id_seq; Type: SEQUENCE; Schema: public; Owner: david
--

CREATE SEQUENCE public.series_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.series_id_seq OWNER TO david;

--
-- Name: series_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: david
--

ALTER SEQUENCE public.series_id_seq OWNED BY public.series.id;


--
-- Name: authors id; Type: DEFAULT; Schema: public; Owner: david
--

ALTER TABLE ONLY public.authors ALTER COLUMN id SET DEFAULT nextval('public.authors_id_seq'::regclass);


--
-- Name: books id; Type: DEFAULT; Schema: public; Owner: david
--

ALTER TABLE ONLY public.books ALTER COLUMN id SET DEFAULT nextval('public.books_id_seq'::regclass);


--
-- Name: series id; Type: DEFAULT; Schema: public; Owner: david
--

ALTER TABLE ONLY public.series ALTER COLUMN id SET DEFAULT nextval('public.series_id_seq'::regclass);


--
-- Data for Name: authors; Type: TABLE DATA; Schema: public; Owner: david
--

COPY public.authors (id, full_name) FROM stdin;
3	Andy Weir
4	Anthony Doerr
5	Arthur C. Clarke
6	Barack Obama
7	Blake Crouch
8	Bret Easton Ellis
9	C. S. Lewis
10	Christopher Buehlman
11	Cormac McCarthy
12	Dan Simmons
13	Frank Herbert
14	J. K. Rowling
15	Jason Rekulak
16	Margaret Atwood
17	Max Brooks
18	Neil Gaiman
19	Nick Cutter
20	Ray Bradbury
21	Robert A. Heinlein
22	Robert Jordan
23	Rumaan Alam
24	Sarah J. Maas
25	Stephanie Garber
26	Stephen King
27	Susanna Clarke
28	Suzanne Collins
29	Tananarive Due
30	V. E. Schwab
31	William Peter Blatty
32	David Wernery
33	ddd
34	ddd
35	rrr
36	rrr
37	ff
38	ddd
39	333
40	vvv
41	555
42	3333
43	ssss
44	11
45	333333
46	4444
\.


--
-- Data for Name: books; Type: TABLE DATA; Schema: public; Owner: david
--

COPY public.books (id, title, pages, author_id, series_id, status, start_date, end_date, current_page, isbn_13, s3_url, rating) FROM stdin;
11	Mockingjay (Hunger Games, Book Three)	390	28	1	UNREAD	2026-05-09	\N	265	0000739216319	\N	\N
17	All the Light We Cannot See	530	4	\N	READ	2023-10-16	2023-11-05	\N	9781501173219	\N	\N
25	'Salem's Lot	751	26	\N	READ	2024-01-14	2024-01-28	\N	9780385528221	\N	\N
26	The Outsider	560	26	\N	READ	2024-01-28	2024-02-03	\N	9781501181009	\N	\N
34	Under the Dome	1072	26	\N	READ	2024-02-16	2024-03-10	\N	9781439149034	\N	\N
38	Starship Troopers	278	21	\N	READ	2024-04-21	2024-04-28	\N	9780441014101	\N	\N
39	No Country for Old Men	309	11	\N	READ	2024-04-28	2024-05-03	\N	9780375706677	\N	\N
48	A Promised Land	701	6	\N	READ	2024-09-21	2024-10-13	\N	9781524763176	\N	\N
49	Piranesi	245	27	\N	READ	2024-10-14	2024-10-15	\N	0001504159680	\N	\N
50	American Gods	522	18	\N	READ	2024-10-16	2024-10-25	\N	9780062572233	\N	\N
53	Something Wicked This Way Comes	304	20	\N	READ	2024-11-08	2024-11-11	\N	9781501167713	\N	\N
57	Run	319	7	\N	READ	2024-12-25	2024-12-31	\N	9780593874806	\N	\N
58	Between Two Fires	434	10	\N	READ	2025-01-02	2025-01-11	\N	9780425256909	\N	\N
59	The Exorcist	378	31	\N	READ	2025-01-12	2025-01-19	\N	9780061007224	\N	\N
60	Needful Things	802	26	\N	READ	2025-01-19	2025-02-08	\N	9781501147418	\N	\N
61	American Psycho	399	8	\N	READ	2025-02-09	2025-02-16	\N	9780679735779	\N	\N
62	Carrie	305	26	\N	READ	2025-02-18	2025-02-22	\N	0000420033149	\N	\N
63	Firestarter	566	26	\N	READ	2025-02-22	2025-03-05	\N	9781501141201	\N	\N
66	The Martian	369	3	\N	READ	2025-03-30	2025-04-06	\N	9780553418026	\N	\N
67	Hidden Pictures	368	15	\N	READ	2025-04-12	2025-04-22	\N	9781250819352	\N	\N
68	Dark Matter	340	7	\N	READ	2025-04-23	2025-05-05	\N	9781101904244	\N	\N
69	The Dead Zone	513	26	\N	READ	2025-05-06	2025-05-24	\N	9781501144509	\N	\N
70	Upgrade	337	7	\N	READ	2025-05-25	2025-05-31	\N	9780593157527	\N	\N
74	Rendezvous with Rama	282	5	\N	READ	2025-07-27	2025-08-06	\N	9780358380221	\N	\N
16	A Court of Silver Flames	751	24	2	READ	2023-08-06	2023-10-13	\N	9781635577990	\N	\N
71	Caraval	403	25	3	READ	2025-05-31	2025-06-10	\N	9781250095268	\N	\N
72	Legendary	443	25	3	READ	2025-06-11	2025-06-20	\N	9781250095329	\N	\N
73	Finale	470	25	3	READ	2025-06-21	2025-07-26	\N	9781250157683	\N	\N
54	Dune	643	13	4	READ	2024-11-11	2024-11-29	\N	9780441013593	\N	\N
55	Dune Messiah	281	13	4	READ	2024-11-29	2024-12-10	\N	9781101157879	\N	\N
56	Children of Dune	477	13	4	READ	2024-12-11	2024-12-25	\N	9781440630514	\N	\N
12	A Court of Thorns and Roses	432	24	2	READ	2023-04-30	2023-05-14	\N	9781619634459	\N	1
13	A Court of Mist and Fury	624	24	2	READ	2023-05-14	2023-06-10	\N	9781526617163	\N	3
14	A Court of Wings and Ruin	699	24	2	READ	2023-06-10	2023-07-29	\N	9781619634497	\N	1
15	A Court of Frost and Starlight	229	24	2	READ	2023-07-29	2023-08-06	\N	9781635575620	\N	5
75	The Troop	355	19	\N	READ	2025-08-06	2025-08-16	\N	9781501144820	\N	\N
76	Recursion	326	7	\N	READ	2025-08-16	2025-08-22	\N	9781524759797	\N	\N
77	Later	248	26	\N	READ	2025-08-23	2025-08-26	\N	9781789096491	\N	\N
79	World War Z	342	17	\N	READ	2025-09-22	2025-10-03	\N	9780307346612	\N	\N
82	Lisey's Story	509	26	\N	READ	2025-12-31	2026-01-18	\N	9781982147792	\N	\N
83	Little Heaven	486	19	\N	READ	2026-01-19	2026-01-30	\N	9781501104237	\N	\N
84	The Abominable	663	12	\N	READ	2026-01-31	2026-02-28	\N	0000599650975	\N	\N
85	Leave the World Behind	241	23	\N	READ	2026-02-28	2026-03-05	\N	0001493825355	\N	\N
86	The Reformatory	565	29	\N	READ	2026-03-06	2026-04-03	\N	0006443780951	\N	\N
87	The Hunger Games (Hunger Games, Book One)	374	28	1	READ	2026-04-04	2026-04-19	\N	0000739209702	\N	\N
88	Catching Fire (Hunger Games, Book Two)	391	28	1	READ	2026-04-22	2026-05-08	\N	0000784460279	\N	\N
51	The Handmaid's Tale	311	16	5	READ	2024-10-25	2024-10-30	\N	9780385490818	\N	\N
52	The Testaments	415	16	5	READ	2024-10-31	2024-11-08	\N	9780385543798	\N	\N
18	Harry Potter and the Sorcerer's Stone	309	14	6	READ	2023-11-05	2023-11-10	\N	9780590353427	\N	\N
19	Harry Potter and the Chamber of Secrets	341	14	6	READ	2023-11-10	2023-11-17	\N	9781781100509	\N	\N
20	Harry Potter and the Prisoner of Azkaban	435	14	6	READ	2023-11-17	2023-11-23	\N	9780439136365	\N	\N
21	Harry Potter and the Goblet of Fire	734	14	6	READ	2023-11-23	2023-12-05	\N	9781338878950	\N	\N
22	Harry Potter and the Order of the Phoenix	870	14	6	READ	2023-12-06	2023-12-19	\N	9781338878967	\N	\N
23	Harry Potter and the Half-Blood Prince	652	14	6	READ	2023-12-20	2023-12-28	\N	9781781100547	\N	\N
24	Harry Potter and the Deathly Hallows	759	14	6	READ	2023-12-28	2024-01-13	\N	9781781102435	\N	\N
64	Hyperion	481	12	7	READ	2025-03-06	2025-03-16	\N	9780399178610	\N	\N
65	The Fall of Hyperion	517	12	7	READ	2025-03-17	2025-03-30	\N	9780307781895	\N	\N
35	A Darker Shade of Magic	398	30	8	READ	2024-03-10	2024-03-17	\N	9780765376466	\N	\N
36	A Gathering of Shadows	509	30	8	READ	2024-03-17	2024-04-07	\N	9780765376480	\N	\N
37	A Conjuring of Light	624	30	8	READ	2024-04-07	2024-04-21	\N	9780765387479	\N	\N
27	The Lion, the Witch and the Wardrobe (full color)	189	9	9	READ	2024-02-03	2024-02-04	\N	9780064409421	\N	\N
28	Prince Caspian (full color)	223	9	9	READ	2024-02-04	2024-02-05	\N	9780064409445	\N	\N
29	The Voyage of the Dawn Treader (full color)	248	9	9	READ	2024-02-05	2024-02-08	\N	9780064409469	\N	\N
30	The Silver Chair (full color)	243	9	9	READ	2024-02-08	2024-02-11	\N	9780064409452	\N	\N
31	The Horse and His Boy (full color)	224	9	9	READ	2024-02-11	2024-02-12	\N	9780064409407	\N	\N
32	The Magician's Nephew (full color)	202	9	9	READ	2024-02-12	2024-02-13	\N	9780064409438	\N	\N
33	The Last Battle (full color)	211	9	9	READ	2024-02-13	2024-02-16	\N	9780064409414	\N	\N
40	The Dark Tower I	251	26	10	READ	2024-05-03	2024-05-10	\N	9781501143519	\N	\N
41	The Dark Tower II	459	26	10	READ	2024-05-11	2024-05-24	\N	9781501143533	\N	\N
42	The Dark Tower III	604	26	10	READ	2024-05-24	2024-06-11	\N	9781501143540	\N	\N
43	The Dark Tower IV	887	26	10	READ	2024-06-12	2024-07-20	\N	9781501143557	\N	\N
44	The Wind Through the Keyhole	307	26	10	READ	2024-07-20	2024-07-27	\N	9781501166228	\N	\N
45	The Dark Tower V	709	26	10	READ	2024-07-27	2024-08-16	\N	9780743251624	\N	\N
46	The Dark Tower VI: Song of Susannah	411	26	10	READ	2024-08-16	2024-08-27	\N	9781848941144	\N	\N
47	The Dark Tower VII	830	26	10	READ	2024-08-27	2024-09-20	\N	9781880418628	\N	\N
78	The Eye of the World	744	22	11	READ	2025-08-27	2025-09-21	\N	9781250768681	\N	\N
80	The Great Hunt	633	22	11	READ	2025-10-03	2025-11-09	\N	9780765334343	\N	\N
81	The Dragon Reborn	586	22	11	READ	2025-11-09	2025-12-28	\N	9780765334350	\N	\N
\.


--
-- Data for Name: series; Type: TABLE DATA; Schema: public; Owner: david
--

COPY public.series (id, name) FROM stdin;
1	The Hunger Games
2	ACOTAR
3	Caraval
4	Dune
5	Handmaid's Tale
6	Harry Potter
7	Hyperion Cantos
8	Shades Of Magic
9	The Chronicles Of Narnia
10	The Dark Tower
11	WoT
12	David's Series
13	ddd
\.


--
-- Name: authors_id_seq; Type: SEQUENCE SET; Schema: public; Owner: david
--

SELECT pg_catalog.setval('public.authors_id_seq', 46, true);


--
-- Name: books_id_seq; Type: SEQUENCE SET; Schema: public; Owner: david
--

SELECT pg_catalog.setval('public.books_id_seq', 88, true);


--
-- Name: series_id_seq; Type: SEQUENCE SET; Schema: public; Owner: david
--

SELECT pg_catalog.setval('public.series_id_seq', 13, true);


--
-- Name: authors authors_pkey; Type: CONSTRAINT; Schema: public; Owner: david
--

ALTER TABLE ONLY public.authors
    ADD CONSTRAINT authors_pkey PRIMARY KEY (id);


--
-- Name: books books_pkey; Type: CONSTRAINT; Schema: public; Owner: david
--

ALTER TABLE ONLY public.books
    ADD CONSTRAINT books_pkey PRIMARY KEY (id);


--
-- Name: series series_pkey; Type: CONSTRAINT; Schema: public; Owner: david
--

ALTER TABLE ONLY public.series
    ADD CONSTRAINT series_pkey PRIMARY KEY (id);


--
-- Name: books author_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: david
--

ALTER TABLE ONLY public.books
    ADD CONSTRAINT author_id_fk FOREIGN KEY (author_id) REFERENCES public.authors(id) NOT VALID;


--
-- Name: books series_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: david
--

ALTER TABLE ONLY public.books
    ADD CONSTRAINT series_id_fk FOREIGN KEY (series_id) REFERENCES public.series(id) NOT VALID;


--
-- PostgreSQL database dump complete
--

