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

