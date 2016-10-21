; Some utility functions that you may find useful.
(define (apply-to-all proc items)
  (if (null? items)
      '()
      (cons (proc (car items))
            (apply-to-all proc (cdr items)))))

(define (cons-all first rests)
  (apply-to-all (lambda (rest) (cons first rest)) rests))

(define (caar x) (car (car x)))
(define (cadr x) (car (cdr x)))
(define (cddr x) (cdr (cdr x)))
(define (cadar x) (car (cdr (car x))))

; Problem 18-
;; Turns a list of pairs into a pair of lists
(define (zip pairs)
  'YOUR-CODE-HERE
  (if (null? pairs) 
      (list '() '())
      (cons (apply-to-all car pairs) (cons (apply-to-all cadr pairs) nil)))))

(zip '((1 2) (3 4) (5 6)))
; expect ((1 3 5) (2 4 6))
(zip '((1 2)))
; expect ((1) (2))
(zip '())
; expect (() ())

; Problem 19

;; List all ways to partition TOTAL without using consecutive numbers.
(define (list-partitions total)
  'YOUR-CODE-HERE
  (define (get_partitions partitions biggest total)
    (cond ((= 0 total) (list partitions))
          ((< total 0) '())
          ((= biggest 0) '())
          (else (append (get_partitions (append partitions (list biggest)) 
                                        biggest 
                                        (- total biggest))
                        (get_partitions partitions (- biggest 1) total)))))
  (define (filter_partition all_partitions)
    (define (no_consec group)
      (cond ((null? group) True)
            ((null? (cdr group)) True)
            ((= 1 (- (car group) (cadr group))) False)
            (else (no_consec (cdr group)))))   
    (cond ((null? all_partitions) '())
          ((no_consec (car all_partitions)) (cons (car all_partitions) 
                                                  (filter_partition (cdr all_partitions))))
          (else (filter_partition (cdr all_partitions)))))
  (filter_partition (get_partitions '() total total)))

; For these two tests, any permutation of the right answer will be accepted.
(list-partitions 5)
; expect ((5) (4 1) (3 1 1) (1 1 1 1 1))
(list-partitions 7)
; expect ((7) (6 1) (5 2) (5 1 1) (4 1 1 1) (3 3 1) (3 1 1 1 1) (1 1 1 1 1 1 1))

; Problem 20
;; Returns a function that takes in an expression and checks if it is the special
;; form FORM
(define (check-special form)
  (lambda (expr) (equal? form (car expr))))

(define lambda? (check-special 'lambda))
(define define? (check-special 'define))
(define quoted? (check-special 'quote))
(define let?    (check-special 'let))

;; Converts all let special forms in EXPR into equivalent forms using lambda
(define (analyze expr)
  (define (apply-analyze items)
    (apply-to-all analyze items))
  (cond ((atom? expr)
         'YOUR-CODE-HERE
         expr
         )
        ((quoted? expr)
         'YOUR-CODE-HERE
         expr
         )
        ((or (lambda? expr)
             (define? expr))
         (let ((form   (car expr))
               (params (cadr expr))
               (body   (cddr expr)))
           'YOUR-CODE-HERE
           (append (list form params) (apply-analyze body))
           ))
        ((let? expr)
         (let ((values (cadr expr))
               (body   (cddr expr)))
           'YOUR-CODE-HERE
           (define zipped_values (zip values))
           (append (list (append (list 'lambda (apply-analyze (car zipped_values)))
                                 (apply-analyze body)))
                   (apply-analyze (cadr zipped_values)))
           ))
        (else
         'YOUR-CODE-HERE
         (apply-analyze expr)
         )))

(analyze 1)
; expect 1
(analyze 'a)
; expect a
(analyze '(+ 1 2))
; expect (+ 1 2)

;; Quoted expressions remain the same
(analyze '(quote (let ((a 1) (b 2)) (+ a b))))
; expect (quote (let ((a 1) (b 2)) (+ a b)))

;; Lambda parameters not affected, but body affected
(analyze '(lambda (let a b) (+ let a b)))
; expect (lambda (let a b) (+ let a b))
(analyze '(lambda (x) a (let ((a x)) a)))
; expect (lambda (x) a ((lambda (a) a) x))

(analyze '(let ((a 1)
                (b 2))
            (+ a b)))
; expect ((lambda (a b) (+ a b)) 1 2)
(analyze '(let ((a (let ((a 2)) a))
                (b 2))
            (+ a b)))
; expect ((lambda (a b) (+ a b)) ((lambda (a) a) 2) 2)
(analyze '(let ((a 1))
            (let ((b a))
              b)))
; expect ((lambda (a) ((lambda (b) b) a)) 1)
(analyze '(+ 1 (let ((a 1)) a)))
; expect (+ 1 ((lambda (a) a) 1))


;; Problem 21 (optional)
;; Draw the hax image using turtle graphics.
(define (hax d k)
  'YOUR-CODE-HERE
  nil)