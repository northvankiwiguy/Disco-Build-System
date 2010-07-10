/*
 * syscall_interpose.c
 *
 *      Author: Peter Smith <psmith@arapiki.com>
 *      Copyright 2010 Arapiki Solutions Inc.
 */
#define _GNU_SOURCE
#include <dlfcn.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdint.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>


/*
 * Fetch a pointer to the real version of the function (if not already defined).
 * We search the list of dynamic libraries to find the next occurrence of this symbol,
 * which should be the "real" version of the function.
 */
#define FETCH_REAL_FN(fn_var, fn_name) \
		static int (*(fn_var))() = NULL; \
		if (!(fn_var)){ \
			(fn_var) = dlsym(RTLD_NEXT, (fn_name)); \
		}


// figure out which syscalls should be traced.
//exit
//fork
//read
//write
//open
//close
//waitpid
//creat
//link
//unlink
//execve
//chdir
//time
//mknod
//chmod
//lchown
//break
//oldstat
//lseek
//getpid
//mount
//umount
//setuid
//getuid
//stime
//ptrace
//alarm
//oldfstat
//pause
//utime
//stty
//gtty
//access
//nice
//ftime
//sync
//kill
//rename
//mkdir
//rmdir
//dup
//pipe
//times
//prof
//brk
//setgid
//getgid
//signal
//geteuid
//getegid
//acct
//umount2
//lock
//ioctl
//fcntl
//mpx
//setpgid
//ulimit
//oldolduname
//umask
//chroot
//ustat
//dup2
//getppid
//getpgrp
//setsid
//sigaction
//sgetmask
//ssetmask
//setreuid           
//setregid           
//sigsuspend         
//sigpending         
//sethostname        
//setrlimit          
//getrlimit          
//getrusage          
//gettimeofday       
//settimeofday       
//getgroups          
//setgroups          
//select             
//symlink            
//oldlstat           
//readlink           
//uselib             
//swapon             
//reboot             
//readdir            
//mmap               
//munmap             
//truncate           
//ftruncate          
//fchmod             
//fchown             
//getpriority        
//setpriority        
//profil             
//statfs             
//fstatfs            
//ioperm             
//socketcall         
//syslog             
//setitimer          
//getitimer          
//stat               
//lstat              
//fstat              
//olduname           
//iopl               
//vhangup            
//idle               
//vm86old            
//wait4              
//swapoff            
//sysinfo            
//ipc                
//fsync              
//sigreturn          
//clone              
//setdomainname      
//uname              
//modify_ldt         
//adjtimex           
//mprotect           
//sigprocmask        
//create_module      
//init_module        
//delete_module      
//get_kernel_syms    
//quotactl           
//getpgid            
//fchdir             
//bdflush            
//sysfs              
//personality        
//afs_syscall        
//setfsuid           
//setfsgid           
//_llseek            
//getdents           
//_newselect         
//flock              
//msync              
//readv              
//writev             
//getsid             
//fdatasync          
//_sysctl            
//mlock              
//munlock            
//mlockall           
//munlockall         
//sched_setparam     
//sched_getparam          
//sched_setscheduler      
//sched_getscheduler      
//sched_yield           
//sched_get_priority_max
//sched_get_priority_min
//sched_rr_get_interval 
//nanosleep          
//mremap             
//setresuid          
//getresuid          
//vm86               
//query_module      
//poll              
//nfsservctl        
//setresgid         
//getresgid         
//prctl             
//rt_sigreturn      
//rt_sigaction      
//rt_sigprocmask    
//rt_sigpending     
//rt_sigtimedwait   
//rt_sigqueueinfo   
//rt_sigsuspend     
//pread64           
//pwrite64          
//chown             
//getcwd            
//capget            
//capset            
//sigaltstack       
//sendfile          
//getpmsg           
//putpmsg           
//vfork             
//ugetrlimit        
//mmap2             
//truncate64        
//ftruncate64       
//stat64            
//lstat64           
//fstat64           
//lchown32          
//getuid32          
//getgid32          
//geteuid32         
//getegid32         
//setreuid32        
//setregid32        
//getgroups32       
//setgroups32       
//fchown32          
//setresuid32       
//getresuid32       
//setresgid32       
//getresgid32       
//chown32           
//setuid32          
//setgid32           
//setfsuid32         
//setfsgid32         
//pivot_root         
//mincore            
//madvise            
//madvise1           
//getdents64         
//fcntl64            
//gettid            
//readahead         
//setxattr          
//lsetxattr         
//fsetxattr          
//getxattr           
//lgetxattr          
//fgetxattr          
//listxattr          
//llistxattr         
//flistxattr         
//removexattr        
//lremovexattr       
//fremovexattr       
//tkill              
//sendfile64         
//futex              
//sched_setaffinity  
//sched_getaffinity  
//set_thread_area    
//get_thread_area    
//io_setup           
//io_destroy         
//io_getevents       
//io_submit          
//io_cancel          
//fadvise64          
//exit_group         
//lookup_dcookie     
//epoll_create       
//epoll_ctl          
//epoll_wait         
//remap_file_pages   
//set_tid_address    
//timer_create       
//timer_settime      
//timer_gettime      
//timer_getoverrun   
//timer_delete       
//clock_settime     
//clock_gettime     
//clock_getres      
//clock_nanosleep   
//statfs64          
//fstatfs64         
//tgkill            
//utimes            
//fadvise64_64      
//vserver           
//mbind             
//get_mempolicy     
//set_mempolicy     
//mq_open           
//mq_unlink         
//mq_timedsend      
//mq_timedreceive   
//mq_notify         
//mq_getsetattr     
//kexec_load       
//waitid           
//add_key          
//request_key      
//keyctl           
//ioprio_set       
//ioprio_get       
//inotify_init     
//inotify_add_watch
//inotify_rm_watch 
//migrate_pages    
//openat           
//mkdirat          
//mknodat        
//fchownat       
//futimesat      
//fstatat64      
//unlinkat       
//renameat       
//linkat         
//symlinkat      
//readlinkat     
//fchmodat       
//faccessat      
//pselect6       
//ppoll          
//unshare        
//set_robust_list
//get_robust_list
//splice         
//sync_file_range
//tee            
//vmsplice       
//move_pages     
//getcpu         
//epoll_pwait    
//utimensat      
//signalfd       
//timerfd_create 
//eventfd        
//fallocate      
//timerfd_settime
//timerfd_gettime
//signalfd4
//eventfd2
//epoll_create1
//dup3
//pipe2
//inotify_init1

/*
 * _init_interposer()
 *
 * When this dynamic library is first loaded, the _init_interposer function is called
 * as a constructor. This is where we allocate the shared memory segment for cfs and
 * perform any other start-up tasks.
 */
void _init_interposer() __attribute__ ((constructor));

void _init_interposer()
{
	//printf("_init_interposer\n");
}

/*======================================================================
 * Interposer functions
 *
 * Each of these functions interposes the "real" system calls. Each
 * function starts by getting a handle to the real system call, then
 * adds extra functionality around the basic call.
 * Notes:
 *   - The real_xxx functions must be determined inside the interposer
 *     function. It's not possible to compute them from inside the
 *     constructor function (_init_interposer), since some of the system
 *     calls will be executed before the constructor is called.
 *   - Functions are listed alphabetically, purely for convenience.
 *======================================================================*/

int open64(const char *pathname, int flags, ...)
{
	// TODO: There are more variants of open to be handled. See fcntl.h.

	FETCH_REAL_FN(real_open64, "open");

	// fetch the optional mode argument.
	va_list ap;
	va_start(ap, &flags);
	mode_t mode = va_arg(ap, mode_t);
	va_end(ap);

	//printf("open64(%s, %d, 0x%x)\n", pathname, flags, mode);
	int fd = real_open64(pathname, flags, mode);
	return fd;
}

/*======================================================================*/

