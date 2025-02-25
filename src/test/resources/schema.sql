CREATE TABLE public.course
(
    id        varchar NOT NULL,
    title     varchar NOT NULL,
    "version" int NULL,
    CONSTRAINT course_pk PRIMARY KEY (id)
);

CREATE TABLE public.student
(
    id        varchar NOT NULL,
    full_name varchar NOT NULL,
    "version" int NULL,
    CONSTRAINT student_pk PRIMARY KEY (id)
);

CREATE TABLE public.subscription
(
    id         varchar NOT NULL,
    student_id varchar NOT NULL,
    course_id  varchar NOT NULL,
    optional   boolean NOT NULL default false,
    "version"  int NULL,
    CONSTRAINT subscription_pk PRIMARY KEY (id),
    CONSTRAINT subscription_student_id_fk FOREIGN KEY (student_id) REFERENCES public.student (id),
    CONSTRAINT subscription_course_id_fk FOREIGN KEY (course_id) REFERENCES public.course (id)
);